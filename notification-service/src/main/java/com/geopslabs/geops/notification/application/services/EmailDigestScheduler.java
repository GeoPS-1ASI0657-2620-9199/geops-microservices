package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.application.ports.EmailSender;
import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.*;
import org.slf4j.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.util.HtmlUtils;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class EmailDigestScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailDigestScheduler.class);
    private static final Duration MAX_RETRY_AGE = Duration.ofHours(24);
    private static final long INTERVAL_MS = 60_000;
    private final EmailDigestRepositoryPort digests;
    private final NoticePlanningRepositoryPort notices;
    private final EmailUnsubscriptionRepositoryPort unsubscriptions;
    private final EmailSender sender;
    private final TransactionOperations transactions;
    private final Clock clock;
    private final String secret;
    private final String webBaseUrl;
    private final String template;

    public EmailDigestScheduler(EmailDigestRepositoryPort digests, NoticePlanningRepositoryPort notices,
                                EmailUnsubscriptionRepositoryPort unsubscriptions, EmailSender sender,
                                TransactionOperations transactions, Clock clock, String secret, String webBaseUrl, String template) {
        this.digests = digests; this.notices = notices; this.unsubscriptions = unsubscriptions; this.sender = sender;
        this.transactions = transactions; this.clock = clock; this.secret = secret; this.webBaseUrl = webBaseUrl; this.template = template;
    }

    public void sendDue() { digests.withSchedulerLock(() -> digests.due(clock.instant()).forEach(this::send)); }

    private void send(EmailDigest digest) {
        var pending = transactions.execute(status -> eligible(digest));
        if (pending == null || pending.isEmpty()) { digests.complete(digest.id(), clock.instant()); return; }
        if (!digest.windowEnd().plus(MAX_RETRY_AGE).isAfter(clock.instant())) { finish(digest, pending, DeliveryStatus.FAILED); return; }
        try {
            var recipient = notices.recipient(digest.consumerId()).orElseThrow();
            sender.send(recipient.email(), "Tus ofertas de GeoPS", render(digest.consumerId(), pending));
            finish(digest, pending, DeliveryStatus.SENT);
            LOGGER.info("notification.email.sent recipientId={} digestId={}", digest.consumerId(), digest.id());
        } catch (RuntimeException error) { LOGGER.warn("notification.email.failed recipientId={} digestId={} reason={}", digest.consumerId(), digest.id(), error.getClass().getSimpleName()); }
    }

    private List<Notification> eligible(EmailDigest digest) {
        var preference = notices.lockPreferences(digest.consumerId());
        var confirmed = notices.recipient(digest.consumerId()).map(r -> Boolean.TRUE.equals(r.emailConfirmed())).orElse(false);
        var pending = digests.notices(digest);
        var eligible = new ArrayList<Notification>();
        for (var notice : pending) {
            var fresh = notice.getType() != NotificationType.NEARBY_OFFER || notices.hasFreshLocation(digest.consumerId(), clock.instant().minus(NotificationPlanner.FRESHNESS));
            if (preference.isPresent() && preference.get().getEmailEnabled() && confirmed && fresh && !unsubscriptions.exists(digest.consumerId(), notice.getType())) {
                if (NotificationPlanner.withinDailyLimit(notices, notice, preference.get().getDailyLimit())) eligible.add(notice);
                else notices.status(notice.getId(), DeliveryStatus.SKIPPED_BY_LIMIT);
            }
            else notices.status(notice.getId(), DeliveryStatus.CANCELLED);
        }
        return eligible;
    }

    private void finish(EmailDigest digest, List<Notification> pending, DeliveryStatus status) {
        transactions.executeWithoutResult(tx -> { pending.forEach(n -> notices.status(n.getId(), status)); digests.complete(digest.id(), clock.instant()); });
    }

    private String render(Long consumerId, List<Notification> pending) {
        var rows = pending.stream().map(n -> "<li>" + HtmlUtils.htmlEscape(n.getMessage()) + "</li>").collect(Collectors.joining());
        var links = pending.stream().map(Notification::getType).distinct().map(type -> "<p><a href=\"" + HtmlUtils.htmlEscape(webBaseUrl + "/baja?token=" + UnsubscribeToken.issue(consumerId, type, secret)) + "\">Dejar de recibir " + HtmlUtils.htmlEscape(type == NotificationType.NEARBY_OFFER ? "ofertas cercanas" : "ofertas de comercios seguidos") + "</a></p>").collect(Collectors.joining());
        return template.replace("{{offers}}", rows).replace("{{unsubscribeLinks}}", links);
    }
}
