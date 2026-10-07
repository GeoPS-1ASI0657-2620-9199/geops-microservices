package com.geopslabs.geops.notification.domain.ports;

import com.geopslabs.geops.notification.domain.models.Notification;

import java.time.Instant;

public interface NotificationRepositoryPort {
    Notification save(Notification notification);

    long countSentSince(Long recipientId, Instant since);
}
