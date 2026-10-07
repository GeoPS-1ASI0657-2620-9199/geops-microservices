package com.geopslabs.geops.notification.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Notification Repository
 *
 * JPA repository for Notification aggregate
 *
 * @summary Repository for notification persistence operations
 * @since 1.0
 * @author GeOps Labs
 */
@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {

    /**
     * Find all notifications for a specific user, ordered by creation date descending
     *
     * @param userId User ID
     * @return List of notifications
     */
    List<NotificationJpaEntity> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    /**
     * Count unread notifications for a user
     *
     * @param userId User ID
     * @param isRead Read status (false for unread)
     * @return Count of unread notifications
     */
    Long countByRecipientIdAndIsRead(Long recipientId, Boolean isRead);

    /**
     * Find all unread notifications for a user
     *
     * @param userId User ID
     * @param isRead Read status
     * @return List of notifications
     */
    List<NotificationJpaEntity> findByRecipientIdAndIsReadOrderByCreatedAtDesc(Long recipientId, Boolean isRead);

    /**
     * Mark all notifications as read for a user
     *
     * @param userId User ID
     * @return Number of updated notifications
     */
    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.isRead = true WHERE n.recipientId = :recipientId AND n.isRead = false")
    int markAllAsReadByRecipientId(@Param("recipientId") Long recipientId);
}
