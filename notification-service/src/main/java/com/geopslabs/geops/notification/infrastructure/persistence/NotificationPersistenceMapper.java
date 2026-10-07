package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.domain.models.Notification;

final class NotificationPersistenceMapper {

    private NotificationPersistenceMapper() {
    }

    static Notification toDomain(NotificationJpaEntity entity) {
        return new Notification(entity.getId(), entity.getUser(), entity.getType(), entity.getTitle(),
                entity.getMessage(), entity.getIsRead(), entity.getRelatedEntityId(), entity.getRelatedEntityType(),
                entity.getActionUrl(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    static NotificationJpaEntity toEntity(Notification notification, NotificationJpaEntity entity) {
        entity.setUser(notification.getUser());
        entity.setType(notification.getType());
        entity.setTitle(notification.getTitle());
        entity.setMessage(notification.getMessage());
        entity.setIsRead(notification.getIsRead());
        entity.setRelatedEntityId(notification.getRelatedEntityId());
        entity.setRelatedEntityType(notification.getRelatedEntityType());
        entity.setActionUrl(notification.getActionUrl());
        return entity;
    }
}
