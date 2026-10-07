package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.domain.models.Notification;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

final class NotificationPersistenceMapper {

    private NotificationPersistenceMapper() {
    }

    static Notification toDomain(NotificationJpaEntity entity) {
        return new Notification(entity.getId(), entity.getRecipientId(), entity.getType(), entity.getChannel(),
                entity.getTitle(), entity.getMessage(), entity.getRelatedEntityId(), entity.getStatus(),
                LocalDateTime.ofInstant(entity.getSentAt(), ZoneOffset.UTC));
    }

    static NotificationJpaEntity toEntity(Notification notification) {
        var entity = new NotificationJpaEntity();
        entity.setId(notification.getId());
        entity.setRecipientId(notification.getRecipientId());
        entity.setType(notification.getType());
        entity.setChannel(notification.getChannel());
        entity.setTitle(notification.getTitle());
        entity.setMessage(notification.getMessage());
        entity.setRelatedEntityId(notification.getRelatedEntityId());
        entity.setStatus(notification.getStatus());
        entity.setSentAt(notification.getSentAt().toInstant(ZoneOffset.UTC));
        return entity;
    }
}
