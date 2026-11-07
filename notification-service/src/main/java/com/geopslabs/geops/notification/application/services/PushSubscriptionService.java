package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.application.usecases.*;
import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.models.exceptions.*;
import com.geopslabs.geops.notification.domain.ports.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

public class PushSubscriptionService implements RegisterPushSubscriptionUseCase, RemovePushSubscriptionUseCase {
    private final PushSubscriptionRepositoryPort subscriptions;
    private final RecipientRepositoryPort recipients;
    private final NotificationPreferenceRepositoryPort preferences;
    private final Clock clock;
    public PushSubscriptionService(PushSubscriptionRepositoryPort subscriptions, RecipientRepositoryPort recipients, NotificationPreferenceRepositoryPort preferences, Clock clock) { this.subscriptions = subscriptions; this.recipients = recipients; this.preferences = preferences; this.clock = clock; }
    @Override
    @Transactional
    public Result register(Long consumerId, String token, String userAgent) {
        if (!recipients.existsConsumer(consumerId)) throw new RecipientNotFoundException();
        preferences.createIfAbsent(NotificationPreference.initialFor(consumerId, LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)));
        var inserted = subscriptions.saveIfAbsent(new PushSubscription(null, consumerId, token, userAgent, clock.instant()));
        var subscription = inserted.orElseGet(() -> subscriptions.findByToken(token).orElseThrow());
        if (!subscription.consumerId().equals(consumerId)) throw new PushSubscriptionOwnershipException();
        return new Result(subscription, inserted.isPresent());
    }
    @Override
    @Transactional
    public void remove(Long consumerId, Long subscriptionId) { subscriptions.delete(subscriptionId, consumerId); }
}
