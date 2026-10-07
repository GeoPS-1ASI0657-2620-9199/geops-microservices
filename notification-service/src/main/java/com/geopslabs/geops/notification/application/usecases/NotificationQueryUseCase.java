package com.geopslabs.geops.notification.application.usecases;

import com.geopslabs.geops.notification.domain.models.Notification;
import com.geopslabs.geops.notification.domain.models.queries.GetNotificationByIdQuery;
import com.geopslabs.geops.notification.domain.models.queries.GetNotificationsByUserIdQuery;
import com.geopslabs.geops.notification.domain.models.queries.GetUnreadCountByUserIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * Notification Query Service
 *
 * Service interface for handling notification query operations
 *
 * @summary Interface for notification query operations
 * @since 1.0
 * @author GeOps Labs
 */
public interface NotificationQueryUseCase {

    /**
     * Gets all notifications for a user
     *
     * @param query The query containing user ID
     * @return List of notifications
     */
    List<Notification> handle(GetNotificationsByUserIdQuery query);

    /**
     * Gets a specific notification by ID
     *
     * @param query The query containing notification ID
     * @return Optional containing the notification
     */
    Optional<Notification> handle(GetNotificationByIdQuery query);

    /**
     * Gets count of unread notifications for a user
     *
     * @param query The query containing user ID
     * @return Count of unread notifications
     */
    Long handle(GetUnreadCountByUserIdQuery query);
}
