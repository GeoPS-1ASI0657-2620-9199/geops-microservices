package com.geopslabs.geops.notification.domain.models;

import java.time.Instant;
public record PushSubscription(Long id, Long consumerId, String fcmToken, String userAgent, Instant createdAt) {}
