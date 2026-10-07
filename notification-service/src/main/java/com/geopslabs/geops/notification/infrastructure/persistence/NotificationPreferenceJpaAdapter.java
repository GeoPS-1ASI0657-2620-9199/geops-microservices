package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.domain.models.NotificationPreference;
import com.geopslabs.geops.notification.domain.ports.NotificationPreferenceRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Component
public class NotificationPreferenceJpaAdapter implements NotificationPreferenceRepositoryPort {
    private final NotificationPreferenceJpaRepository repository;

    public NotificationPreferenceJpaAdapter(NotificationPreferenceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<NotificationPreference> findByConsumerId(Long consumerId) {
        return repository.findById(consumerId).map(NotificationPreferenceJpaAdapter::toDomain);
    }

    @Override
    public NotificationPreference upsert(NotificationPreference preference) {
        repository.upsert(preference.getConsumerId(), preference.getPushEnabled(), preference.getEmailEnabled(),
                preference.getDailyLimit(), preference.getUpdatedAt().toInstant(ZoneOffset.UTC));
        return preference;
    }

    @Override
    public void createIfAbsent(NotificationPreference preference) {
        repository.insertIfAbsent(preference.getConsumerId(), preference.getPushEnabled(),
                preference.getEmailEnabled(), preference.getDailyLimit(),
                preference.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    private static NotificationPreference toDomain(NotificationPreferenceJpaEntity entity) {
        return new NotificationPreference(entity.getConsumerId(), entity.getPushEnabled(), entity.getEmailEnabled(),
                entity.getDailyLimit(), LocalDateTime.ofInstant(entity.getUpdatedAt(), ZoneOffset.UTC));
    }
}
