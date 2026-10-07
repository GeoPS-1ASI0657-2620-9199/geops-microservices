package com.geopslabs.geops.notification.domain.ports;

import com.geopslabs.geops.notification.domain.models.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepositoryPort {
    Notification save(Notification notification);

    Optional<Notification> findById(Long id);

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    Long countByUserIdAndIsRead(Long userId, Boolean isRead);

    boolean existsById(Long id);

    void deleteById(Long id);

    int markAllAsReadByUserId(Long userId);
}
