package com.geopslabs.geops.notification.domain.models;

import com.geopslabs.geops.notification.domain.models.exceptions.InvalidDailyLimitException;

import java.time.LocalDateTime;

public class NotificationPreference {
    public static final int MIN_DAILY_LIMIT = 1;
    public static final int MAX_DAILY_LIMIT = 10;
    public static final int INITIAL_DAILY_LIMIT = 3;

    private final Long consumerId;
    private final Boolean pushEnabled;
    private final Boolean emailEnabled;
    private final Integer dailyLimit;
    private final LocalDateTime updatedAt;

    public NotificationPreference(Long consumerId, Boolean pushEnabled, Boolean emailEnabled, Integer dailyLimit,
                                  LocalDateTime updatedAt) {
        if (dailyLimit == null || dailyLimit < MIN_DAILY_LIMIT || dailyLimit > MAX_DAILY_LIMIT) {
            throw new InvalidDailyLimitException();
        }
        this.consumerId = consumerId;
        this.pushEnabled = pushEnabled;
        this.emailEnabled = emailEnabled;
        this.dailyLimit = dailyLimit;
        this.updatedAt = updatedAt;
    }

    public static NotificationPreference initialFor(Long consumerId, LocalDateTime now) {
        return new NotificationPreference(consumerId, Boolean.FALSE, Boolean.FALSE, INITIAL_DAILY_LIMIT, now);
    }

    public boolean allows(Channel channel, int sentToday) {
        return isEnabled(channel) && sentToday < dailyLimit;
    }

    private boolean isEnabled(Channel channel) {
        return channel == Channel.WEB_PUSH ? pushEnabled : emailEnabled;
    }

    public Long getConsumerId() {
        return consumerId;
    }

    public Boolean getPushEnabled() {
        return pushEnabled;
    }

    public Boolean getEmailEnabled() {
        return emailEnabled;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
