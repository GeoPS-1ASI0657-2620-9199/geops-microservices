package com.geopslabs.geops.notification.infrastructure.web;

import com.geopslabs.geops.notification.domain.models.Notification;
import com.geopslabs.geops.notification.infrastructure.web.NotificationResource;

/**
 * Notification Resource From Entity Assembler
 *
 * Assembler to transform Notification entity to NotificationResource
 *
 * @summary Assembler for notification entity to resource transformation
 * @since 1.0
 * @author GeOps Labs
 */
public class NotificationResourceFromEntityAssembler {

    /**
     * Transform Notification entity to NotificationResource
     *
     * @param entity Notification entity
     * @return NotificationResource
     */
    public static NotificationResource toResourceFromEntity(Notification entity) {
        return new NotificationResource(
            entity.getId(),
            entity.getRecipientId(),
            entity.getType(),
            entity.getTitle(),
            entity.getMessage(),
            entity.getIsRead(),
            entity.getRelatedEntityId(),
            entity.getRelatedEntityType(),
            entity.getActionUrl(),
            entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null
        );
    }
}
