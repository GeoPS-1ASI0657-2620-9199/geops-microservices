package com.geopslabs.geops.notification.domain.models;

import java.time.Instant;
public record EmailUnsubscription(Long consumerId, NotificationType notificationType, String token, Instant unsubscribedAt) {}
