package com.geopslabs.geops.notification.infrastructure.web;

import com.geopslabs.geops.notification.domain.models.NotificationPreference;

import java.time.Instant;
import java.time.ZoneOffset;

public record PreferencesResponse(Long consumerId, boolean pushEnabled, boolean emailEnabled, Integer dailyLimit,
                                  Instant updatedAt) {

    public static PreferencesResponse from(NotificationPreference preference) {
        return new PreferencesResponse(preference.getConsumerId(), preference.getPushEnabled(),
                preference.getEmailEnabled(), preference.getDailyLimit(),
                preference.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }
}
