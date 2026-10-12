package com.geopslabs.geops.identity.infrastructure.sunat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.util.concurrent.ScheduledFuture;

/**
 * When the registry is loaded (E2-12): every day at 04:00 in Lima, and at startup when the last load is older than
 * 24 hours, because the VM spends nights switched off. A failed load is tried again one hour later; meanwhile the
 * verification keeps using the previous registry.
 */
public class SunatRegistrySchedule {
    private static final Logger LOGGER = LoggerFactory.getLogger(SunatRegistrySchedule.class);

    private final SunatRegistryLoader loader;
    private final TaskScheduler taskScheduler;
    private final SunatProperties properties;
    private final Clock clock;
    private ScheduledFuture<?> pendingRetry;

    public SunatRegistrySchedule(SunatRegistryLoader loader, TaskScheduler taskScheduler, SunatProperties properties,
                                 Clock clock) {
        this.loader = loader;
        this.taskScheduler = taskScheduler;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "${geops.sunat.cron:0 0 4 * * *}", zone = "America/Lima")
    public void loadDaily() {
        loadOrRetryLater();
    }

    /** Runs on the scheduler so the service answers while the registry downloads. */
    @EventListener(ApplicationReadyEvent.class)
    public void loadAtStartupWhenStale() {
        taskScheduler.schedule(() -> {
            if (isStale()) {
                loadOrRetryLater();
            } else {
                LOGGER.info("sunat.registry.load.not-needed reason=loaded-within-max-age");
            }
        }, clock.instant());
    }

    boolean isStale() {
        return loader.lastLoadedAt()
                .map(lastLoad -> lastLoad.plus(properties.maxAge()).isBefore(clock.instant()))
                .orElse(true);
    }

    synchronized void loadOrRetryLater() {
        cancelPendingRetry();
        try {
            loader.load();
        } catch (RuntimeException exception) {
            var retryAt = clock.instant().plus(properties.retryDelay());
            pendingRetry = taskScheduler.schedule(this::loadOrRetryLater, retryAt);
            LOGGER.warn("sunat.registry.load.retry-scheduled at={}", retryAt);
        }
    }

    private void cancelPendingRetry() {
        if (pendingRetry != null && !pendingRetry.isDone()) {
            pendingRetry.cancel(false);
        }
        pendingRetry = null;
    }
}
