package com.geopslabs.geops.notification.domain.ports;

import com.geopslabs.geops.notification.domain.models.NotificationPreference;

import java.util.Optional;

public interface NotificationPreferenceRepositoryPort {
    Optional<NotificationPreference> findByConsumerId(Long consumerId);

    NotificationPreference upsert(NotificationPreference preference);

    void createIfAbsent(NotificationPreference preference);
}
