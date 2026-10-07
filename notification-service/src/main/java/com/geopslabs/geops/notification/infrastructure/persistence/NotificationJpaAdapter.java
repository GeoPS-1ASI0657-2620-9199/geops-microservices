package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.domain.models.Notification;
import com.geopslabs.geops.notification.domain.ports.NotificationRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class NotificationJpaAdapter implements NotificationRepositoryPort {
    private final NotificationJpaRepository repository;

    public NotificationJpaAdapter(NotificationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Notification save(Notification notification) {
        var entity = notification.getId() == null ? new NotificationJpaEntity()
                : repository.findById(notification.getId()).orElseGet(NotificationJpaEntity::new);
        return NotificationPersistenceMapper.toDomain(
                repository.save(NotificationPersistenceMapper.toEntity(notification, entity)));
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return repository.findById(id).map(NotificationPersistenceMapper::toDomain);
    }

    @Override
    public List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return repository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Long countByUserIdAndIsRead(Long userId, Boolean isRead) {
        return repository.countByUser_IdAndIsRead(userId, isRead);
    }

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public int markAllAsReadByUserId(Long userId) {
        return repository.markAllAsReadByUserId(userId);
    }
}
