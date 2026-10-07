package com.geopslabs.geops.notification.domain.models.queries;

/**
 * Get Notification By ID Query
 *
 * Query to retrieve a specific notification by its ID
 *
 * @param notificationId Notification ID
 * @summary Query for getting notification by ID
 * @since 1.0
 * @author GeOps Labs
 */
public record GetNotificationByIdQuery(Long notificationId) {
    public GetNotificationByIdQuery {
        if (notificationId == null) {
            throw new IllegalArgumentException("Notification ID cannot be null");
        }
    }
}
