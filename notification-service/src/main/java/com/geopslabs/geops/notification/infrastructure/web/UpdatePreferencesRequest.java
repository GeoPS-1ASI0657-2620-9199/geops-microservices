package com.geopslabs.geops.notification.infrastructure.web;

import com.geopslabs.geops.notification.domain.models.NotificationPreference;
import com.geopslabs.geops.notification.domain.models.commands.UpdatePreferencesCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdatePreferencesRequest(
        @NotNull Boolean pushEnabled,
        @NotNull Boolean emailEnabled,
        @NotNull @Min(NotificationPreference.MIN_DAILY_LIMIT) @Max(NotificationPreference.MAX_DAILY_LIMIT)
        Integer dailyLimit) {

    public UpdatePreferencesCommand toCommand(Long consumerId) {
        return new UpdatePreferencesCommand(consumerId, pushEnabled, emailEnabled, dailyLimit);
    }
}
