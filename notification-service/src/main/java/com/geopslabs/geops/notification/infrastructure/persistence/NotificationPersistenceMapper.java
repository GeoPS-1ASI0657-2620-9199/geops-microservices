package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.domain.models.Notification;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

final class NotificationPersistenceMapper {

    private NotificationPersistenceMapper() {
    }

    static Notification toDomain(NotificationJpaEntity entity) {
        return new Notification(entity.getId(), entity.getRecipientId(), entity.getType(), entity.getTitle(),
                entity.getMessage(), entity.getRelatedEntityId(), toUtc(entity.getCreatedAt()));
    }

    static NotificationJpaEntity toEntity(Notification notification) {
        var entity = new NotificationJpaEntity();
        entity.setId(notification.getId());
        entity.setRecipientId(notification.getRecipientId());
        entity.setType(notification.getType());
        entity.setTitle(notification.getTitle());
        entity.setMessage(notification.getMessage());
        entity.setRelatedEntityId(notification.getRelatedEntityId());
        entity.setCreatedAt(toInstant(notification.getCreatedAt()));
        return entity;
    }

    private static LocalDateTime toUtc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC);
    }
}
