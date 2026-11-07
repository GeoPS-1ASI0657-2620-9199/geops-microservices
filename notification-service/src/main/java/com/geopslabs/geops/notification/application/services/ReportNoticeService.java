package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.application.ports.NotificationChannelStrategy;
import com.geopslabs.geops.notification.application.usecases.NotifyReportPartiesUseCase;
import com.geopslabs.geops.notification.application.exceptions.MissingLocalCopyException;
import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.NoticePlanningRepositoryPort;
import org.springframework.transaction.support.TransactionOperations;
import java.time.*;
import java.util.List;

public class ReportNoticeService implements NotifyReportPartiesUseCase {
    private final NoticePlanningRepositoryPort notices;
    private final List<NotificationChannelStrategy> channels;
    private final TransactionOperations transactions;
    private final Clock clock;
    public ReportNoticeService(NoticePlanningRepositoryPort notices, List<NotificationChannelStrategy> channels, TransactionOperations transactions, Clock clock) { this.notices = notices; this.channels = channels; this.transactions = transactions; this.clock = clock; }
    @Override
    public void apply(ReportNoticeEvent event) {
        switch (event.eventType()) {
            case "ReportOpened" -> notifyOwner(event, NotificationType.REPORT_RECEIVED);
            case "ReportAnswered" -> notifyUser(event.consumerId(), event, NotificationType.REPORT_ANSWERED);
            case "ReportResolved" -> { notifyUser(event.consumerId(), event, NotificationType.REPORT_RESOLVED); notifyOwner(event, NotificationType.REPORT_RESOLVED); }
            default -> { }
        }
    }
    private void notifyOwner(ReportNoticeEvent event, NotificationType type) {
        var owner = notices.businessOwner(event.businessId()).orElseThrow(() -> new MissingLocalCopyException("business owner", event.businessId()));
        notifyUser(owner.userId(), event, type);
    }
    private void notifyUser(Long userId, ReportNoticeEvent event, NotificationType type) {
        var recipient = notices.recipient(userId).orElseThrow(() -> new MissingLocalCopyException("recipient", userId));
        var notice = transactions.execute(tx -> plan(recipient, event, type));
        if (notice == null || notice.getStatus() == DeliveryStatus.SENT) return;
        var email = channels.stream().filter(c -> c.channel() == Channel.EMAIL).findFirst().orElseThrow();
        var result = email.send(notice, recipient);
        if (notices.hasPushSubscription(userId)) channels.stream().filter(c -> c.channel() == Channel.WEB_PUSH && c.available()).findFirst().ifPresent(c -> c.send(notice, recipient));
        notices.status(notice.getId(), result);
        if (result != DeliveryStatus.SENT) throw new IllegalStateException("Report email delivery failed for notice " + notice.getId());
    }
    private Notification plan(Recipient recipient, ReportNoticeEvent event, NotificationType type) {
        notices.lockRecipient(recipient.userId());
        var relatedId = "report-" + event.reportId();
        var existing = notices.caseNotice(recipient.userId(), type, relatedId);
        if (existing.isPresent()) return existing.get();
        return notices.save(NotificationFactoryService.report(recipient.userId(), event, type, clock.instant()));
    }
}
