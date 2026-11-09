package com.geopslabs.geops.reservation.infrastructure.scheduling;

import com.geopslabs.geops.reservation.application.services.DeadlineScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the deadline scheduler every few minutes. Off in the acceptance suite, which runs it by hand. */
@Component
@ConditionalOnProperty(name = "geops.reports.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class DeadlineSchedulerJob {
    private final DeadlineScheduler scheduler;

    public DeadlineSchedulerJob(DeadlineScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Scheduled(fixedDelayString = "${geops.reports.deadline-check-ms:300000}")
    public void run() {
        scheduler.runOnce();
    }
}
