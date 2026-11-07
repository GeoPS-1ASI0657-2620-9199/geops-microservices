package com.geopslabs.geops.notification.domain.ports;

import com.geopslabs.geops.notification.domain.models.*;
import java.time.Instant;
import java.util.*;
public interface NoticePlanningRepositoryPort {
    void lockRecipient(Long recipientId);
    Optional<Notification> caseNotice(Long recipientId, NotificationType type, String relatedId);
    List<Long> followers(Long businessId);
    List<NearbyConsumer> nearby(OfferPublishedEvent offer, Instant since);
    Optional<NotificationPreference> lockPreferences(Long consumerId);
    Optional<Recipient> recipient(Long userId);
    Optional<Recipient> businessOwner(Long businessId);
    boolean hasPushSubscription(Long userId);
    boolean hasFreshLocation(Long userId, Instant since);
    boolean existsOfferNotice(Long consumerId, Long offerId);
    long countOfferNoticesSince(Long consumerId, Instant start, Instant end);
    long countEarlierOrSentOfferNotices(Notification notice, Instant start, Instant end);
    Notification save(Notification notice);
    Optional<Notification> findNotice(Long id);
    void status(Long noticeId, DeliveryStatus status);
    void fallbackToEmail(Long noticeId);
    void cancelOffers(Collection<Long> offerIds);
    boolean existsCaseNotice(Long recipientId, NotificationType type, String relatedId);
}
