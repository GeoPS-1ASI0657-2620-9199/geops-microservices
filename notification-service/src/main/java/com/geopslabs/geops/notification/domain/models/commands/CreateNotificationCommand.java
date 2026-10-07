package com.geopslabs.geops.notification.domain.models.commands;

import com.geopslabs.geops.notification.domain.models.NotificationType;

/**
 * Create Notification Command
 *
 * Command to create a new notification in the system
 *
 * @param recipientId The recipient of the notification
 * @param type Type of notification
 * @param title Notification title
 * @param message Notification message
 * @param relatedEntityId ID of related entity (optional)
 * @param relatedEntityType Type of related entity (optional)
 * @param actionUrl URL to navigate when clicked (optional)
 * @summary Command for creating notifications
 * @since 1.0
 * @author GeOps Labs
 */
public record CreateNotificationCommand(
    Long recipientId,
    NotificationType type,
    String title,
    String message,
    String relatedEntityId,
    String relatedEntityType,
    String actionUrl
) {
    public CreateNotificationCommand {
        if (recipientId == null) {
            throw new IllegalArgumentException("Recipient ID cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Notification type cannot be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
    }
}
