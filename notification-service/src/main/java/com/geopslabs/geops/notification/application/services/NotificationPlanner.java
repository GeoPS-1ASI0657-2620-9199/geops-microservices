package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.application.ports.NotificationChannelStrategy;
import com.geopslabs.geops.notification.application.usecases.*;
import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.*;
import org.springframework.transaction.support.TransactionOperations;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class NotificationPlanner implements PlanOfferNoticesUseCase, CancelOfferNoticesUseCase {
    public static final Duration FRESHNESS = Duration.ofMinutes(60);
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM");
    private final NoticePlanningRepositoryPort repository;
    private final EmailUnsubscriptionRepositoryPort unsubscriptions;
    private final EmailDigestRepositoryPort digestRepository;
    private final EmailDigestService digests;
    private final List<NotificationChannelStrategy> channels;
    private final TransactionOperations transactions;
    private final Clock clock;

    public NotificationPlanner(NoticePlanningRepositoryPort repository, EmailUnsubscriptionRepositoryPort unsubscriptions,
                               EmailDigestRepositoryPort digestRepository, EmailDigestService digests,
                               List<NotificationChannelStrategy> channels, TransactionOperations transactions, Clock clock) {
        this.repository = repository; this.unsubscriptions = unsubscriptions; this.digestRepository = digestRepository;
        this.digests = digests; this.channels = channels; this.transactions = transactions; this.clock = clock;
    }

    @Override
    public void offerPublished(OfferPublishedEvent offer) {
        if (offer.occurredAt().isBefore(clock.instant().minus(FRESHNESS)) || offer.validTo().isBefore(today())) return;
        var followers = new HashSet<>(repository.followers(offer.businessId()));
        var nearby = repository.nearby(offer, clock.instant().minus(FRESHNESS));
        var candidates = new LinkedHashMap<Long, NearbyConsumer>();
        nearby.forEach(candidate -> candidates.put(candidate.consumerId(), candidate));
        followers.forEach(id -> candidates.putIfAbsent(id, new NearbyConsumer(id, null)));
        candidates.values().forEach(candidate -> planAndSend(candidate, followers.contains(candidate.consumerId()), offer));
    }

    private void planAndSend(NearbyConsumer candidate, boolean follower, OfferPublishedEvent offer) {
        var type = follower ? NotificationType.FOLLOWED_BUSINESS_OFFER : NotificationType.NEARBY_OFFER;
        var notice = transactions.execute(status -> planOne(candidate, type, offer));
        if (notice != null && notice.getChannel() == Channel.WEB_PUSH && notice.getStatus() == DeliveryStatus.PENDING) sendPush(notice, offer.offerId());
    }

    private Notification planOne(NearbyConsumer candidate, NotificationType type, OfferPublishedEvent offer) {
        var consumerId = candidate.consumerId();
        var preference = repository.lockPreferences(consumerId);
        if (preference.isEmpty() || repository.existsOfferNotice(consumerId, offer.offerId())) return null;
        var dayStart = today().atStartOfDay(LIMA).toInstant();
        var dayEnd = today().plusDays(1).atStartOfDay(LIMA).toInstant();
        if (repository.countOfferNoticesSince(consumerId, dayStart, dayEnd) >= preference.get().getDailyLimit()) {
            return repository.save(notice(consumerId, type, offer, candidate.meters(), Channel.EMAIL, DeliveryStatus.SKIPPED_BY_LIMIT));
        }
        var channel = channelFor(preference.get(), type);
        if (channel.isEmpty()) return null;
        var notice = repository.save(notice(consumerId, type, offer, candidate.meters(), channel.get(), DeliveryStatus.PENDING));
        if (channel.get() == Channel.EMAIL) digests.addToCurrentWindow(consumerId, offer.offerId(), clock.instant());
        return notice;
    }

    private Optional<Channel> channelFor(NotificationPreference preference, NotificationType type) {
        if (preference.getPushEnabled() && repository.hasPushSubscription(preference.getConsumerId()) && pushChannel().isPresent()) return Optional.of(Channel.WEB_PUSH);
        return emailAllowed(preference, type) ? Optional.of(Channel.EMAIL) : Optional.empty();
    }

    private boolean emailAllowed(NotificationPreference preference, NotificationType type) {
        return preference.getEmailEnabled() && !unsubscriptions.exists(preference.getConsumerId(), type)
                && repository.recipient(preference.getConsumerId()).map(r -> Boolean.TRUE.equals(r.emailConfirmed())).orElse(false);
    }

    private void sendPush(Notification notice, Long offerId) {
        var permitted = transactions.execute(status -> permittedNow(notice));
        if (!Boolean.TRUE.equals(permitted)) return;
        var recipient = repository.recipient(notice.getRecipientId()).orElseThrow();
        var result = pushChannel().map(channel -> channel.send(notice, recipient)).orElse(DeliveryStatus.FAILED);
        transactions.executeWithoutResult(status -> finishPush(notice, offerId, result));
    }

    private boolean permittedNow(Notification notice) {
        var preference = repository.lockPreferences(notice.getRecipientId());
        var current = repository.findNotice(notice.getId());
        var fresh = notice.getType() != NotificationType.NEARBY_OFFER || repository.hasFreshLocation(notice.getRecipientId(), clock.instant().minus(FRESHNESS));
        if (current.isEmpty() || current.get().getStatus() != DeliveryStatus.PENDING) return false;
        if (preference.isPresent() && preference.get().getPushEnabled() && fresh) {
            if (withinDailyLimit(repository, notice, preference.get().getDailyLimit())) return true;
            repository.status(notice.getId(), DeliveryStatus.SKIPPED_BY_LIMIT);
            return false;
        }
        repository.status(notice.getId(), DeliveryStatus.CANCELLED);
        return false;
    }

    private void finishPush(Notification notice, Long offerId, DeliveryStatus result) {
        var preference = repository.lockPreferences(notice.getRecipientId());
        if (repository.findNotice(notice.getId()).map(n -> n.getStatus() != DeliveryStatus.PENDING).orElse(true)) return;
        if (result == DeliveryStatus.FAILED && preference.isPresent() && emailAllowed(preference.get(), notice.getType())) {
            repository.fallbackToEmail(notice.getId());
            digests.addToCurrentWindow(notice.getRecipientId(), offerId, clock.instant());
            return;
        }
        repository.status(notice.getId(), result);
    }

    private Optional<NotificationChannelStrategy> pushChannel() { return channels.stream().filter(c -> c.channel() == Channel.WEB_PUSH && c.available()).findFirst(); }

    private Notification notice(Long consumerId, NotificationType type, OfferPublishedEvent offer, Integer meters, Channel channel, DeliveryStatus status) {
        return NotificationFactoryService.offer(consumerId, type, offer, meters, channel, status, clock.instant());
    }

    static boolean withinDailyLimit(NoticePlanningRepositoryPort repository, Notification notice, int limit) {
        var day = notice.getSentAt().toInstant(ZoneOffset.UTC).atZone(LIMA).toLocalDate();
        return repository.countEarlierOrSentOfferNotices(notice, day.atStartOfDay(LIMA).toInstant(), day.plusDays(1).atStartOfDay(LIMA).toInstant()) <= limit;
    }

    private LocalDate today() { return LocalDate.now(clock.withZone(LIMA)); }

    @Override
    public void offerUnavailable(Collection<Long> offerIds) {
        transactions.executeWithoutResult(status -> { repository.cancelOffers(offerIds); digestRepository.removeOffers(offerIds); });
    }
}
