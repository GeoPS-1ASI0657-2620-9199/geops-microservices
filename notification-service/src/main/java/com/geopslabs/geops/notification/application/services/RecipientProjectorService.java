package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.application.usecases.*;
import com.geopslabs.geops.notification.application.exceptions.MissingLocalCopyException;
import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

public class RecipientProjectorService implements ApplyIdentityEventsUseCase, ApplyFollowEventsUseCase {
    private static final int METERS_PER_MINUTE = 80;
    private final RecipientRepositoryPort recipients;
    private final NotificationPreferenceRepositoryPort preferences;
    private final RecipientProjectionPort projections;
    public RecipientProjectorService(RecipientRepositoryPort recipients, NotificationPreferenceRepositoryPort preferences, RecipientProjectionPort projections) {
        this.recipients = recipients; this.preferences = preferences; this.projections = projections;
    }

    @Override
    @Transactional
    public void userRegistered(Recipient recipient, Instant occurredAt) {
        recipients.upsert(recipient);
        if (Recipient.CONSUMER.equals(recipient.role())) preferences.createIfAbsent(NotificationPreference.initialFor(recipient.userId(), LocalDateTime.ofInstant(occurredAt, ZoneOffset.UTC)));
    }

    @Override
    @Transactional
    public void consumerPreferencesChanged(Long consumerId, int searchRadiusMinutes) { projections.updateRadius(consumerId, Math.multiplyExact(searchRadiusMinutes, METERS_PER_MINUTE)); }

    @Override
    @Transactional
    public void businessFollowed(Long consumerId, Long businessId) {
        if (!recipients.existsConsumer(consumerId)) throw new MissingLocalCopyException("recipient", consumerId);
        projections.follow(consumerId, businessId);
    }

    @Override
    @Transactional
    public void businessUnfollowed(Long consumerId, Long businessId) { projections.unfollow(consumerId, businessId); }
}
