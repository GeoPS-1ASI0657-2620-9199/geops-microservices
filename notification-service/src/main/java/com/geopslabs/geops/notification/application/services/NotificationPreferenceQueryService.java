package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.application.usecases.GetNotificationPreferencesUseCase;
import com.geopslabs.geops.notification.domain.models.NotificationPreference;
import com.geopslabs.geops.notification.domain.models.exceptions.RecipientNotFoundException;
import com.geopslabs.geops.notification.domain.ports.NotificationPreferenceRepositoryPort;

public class NotificationPreferenceQueryService implements GetNotificationPreferencesUseCase {
    private final NotificationPreferenceRepositoryPort preferences;
    public NotificationPreferenceQueryService(NotificationPreferenceRepositoryPort preferences) { this.preferences = preferences; }
    @Override
    public NotificationPreference get(Long consumerId) { return preferences.findByConsumerId(consumerId).orElseThrow(RecipientNotFoundException::new); }
}
