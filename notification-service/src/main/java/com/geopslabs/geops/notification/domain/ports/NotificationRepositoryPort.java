package com.geopslabs.geops.notification.domain.ports;

import com.geopslabs.geops.notification.domain.models.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepositoryPort {
    Notification save(Notification notification);

    Optional<Notification> findById(Long id);

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    Long countByRecipientIdAndIsRead(Long recipientId, Boolean isRead);

    boolean existsById(Long id);

    void deleteById(Long id);

    int markAllAsReadByRecipientId(Long recipientId);
}
