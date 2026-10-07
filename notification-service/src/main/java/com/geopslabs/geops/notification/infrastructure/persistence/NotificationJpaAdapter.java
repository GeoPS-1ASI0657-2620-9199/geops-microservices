package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.domain.models.Notification;
import com.geopslabs.geops.notification.domain.ports.NotificationRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class NotificationJpaAdapter implements NotificationRepositoryPort {
    private final NotificationJpaRepository repository;

    public NotificationJpaAdapter(NotificationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Notification save(Notification notification) {
        var entity = NotificationPersistenceMapper.toEntity(notification);
        return NotificationPersistenceMapper.toDomain(repository.save(entity));
    }
}
