package com.geopslabs.geops.notification.infrastructure.scheduling;

import com.geopslabs.geops.notification.application.services.EmailDigestScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name = "geops.notifications.email-scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class EmailDigestJob {
    private static final long INTERVAL_MS = 60_000;
    private final EmailDigestScheduler scheduler;
    public EmailDigestJob(EmailDigestScheduler scheduler) { this.scheduler = scheduler; }
    @Scheduled(fixedDelay = INTERVAL_MS)
    public void run() { scheduler.sendDue(); }
}
