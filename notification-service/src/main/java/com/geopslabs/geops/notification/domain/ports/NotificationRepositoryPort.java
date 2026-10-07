package com.geopslabs.geops.notification.domain.ports;

import com.geopslabs.geops.notification.domain.models.Notification;

public interface NotificationRepositoryPort {
    Notification save(Notification notification);
}
