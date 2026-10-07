package com.geopslabs.geops.notification.domain.models.commands;

public record UpdatePreferencesCommand(Long consumerId, Boolean pushEnabled, Boolean emailEnabled,
                                       Integer dailyLimit) {
}
