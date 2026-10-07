package com.geopslabs.geops.notification.infrastructure.web;

import com.geopslabs.geops.notification.domain.models.NotificationType;

/**
 * Notification Resource
 *
 * REST resource representing a notification
 *
 * @param id Notification ID
 * @param userId User ID who owns the notification
 * @param type Notification type
 * @param title Notification title
 * @param message Notification message
 * @param isRead Read status
 * @param relatedEntityId Related entity ID
 * @param relatedEntityType Related entity type
 * @param actionUrl Action URL
 * @param createdAt Creation timestamp
 * @summary REST resource for notification data
 * @since 1.0
 * @author GeOps Labs
 */
public record NotificationResource(
    Long id,
    Long recipientId,
    NotificationType type,
    String title,
    String message,
    Boolean isRead,
    String relatedEntityId,
    String relatedEntityType,
    String actionUrl,
    String createdAt
) {}
