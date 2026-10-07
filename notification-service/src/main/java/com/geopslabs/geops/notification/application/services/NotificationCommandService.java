package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.domain.models.Notification;
import com.geopslabs.geops.notification.domain.models.commands.CreateNotificationCommand;
import com.geopslabs.geops.notification.domain.models.commands.DeleteNotificationCommand;
import com.geopslabs.geops.notification.domain.models.commands.MarkNotificationAsReadCommand;
import com.geopslabs.geops.notification.application.usecases.NotificationCommandUseCase;
import com.geopslabs.geops.notification.domain.ports.NotificationRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Notification Command Service Implementation
 *
 * Implementation of notification command service operations
 *
 * @summary Implementation of notification command operations
 * @since 1.0
 * @author GeOps Labs
 */
@Transactional
public class NotificationCommandService implements NotificationCommandUseCase {

    private final NotificationRepositoryPort notificationRepository;

    public NotificationCommandService(NotificationRepositoryPort notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Optional<Notification> handle(CreateNotificationCommand command) {
        try {
            var notification = new Notification(command);
            var savedNotification = notificationRepository.save(notification);
            return Optional.of(savedNotification);
        } catch (Exception e) {
            System.err.println("Error creating notification: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<Notification> handle(MarkNotificationAsReadCommand command) {
        try {
            var notificationOptional = notificationRepository.findById(command.notificationId());
            
            if (notificationOptional.isEmpty()) {
                System.err.println("Notification with ID " + command.notificationId() + " not found");
                return Optional.empty();
            }

            var notification = notificationOptional.get();
            notification.markAsRead();
            var updatedNotification = notificationRepository.save(notification);
            return Optional.of(updatedNotification);
        } catch (Exception e) {
            System.err.println("Error marking notification as read: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean handle(DeleteNotificationCommand command) {
        try {
            if (!notificationRepository.existsById(command.notificationId())) {
                System.err.println("Notification with ID " + command.notificationId() + " not found");
                return false;
            }

            notificationRepository.deleteById(command.notificationId());
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting notification: " + e.getMessage());
            return false;
        }
    }

    @Override
    public int markAllAsReadForUser(Long userId) {
        try {
            return notificationRepository.markAllAsReadByRecipientId(userId);
        } catch (Exception e) {
            System.err.println("Error marking all notifications as read for user: " + e.getMessage());
            return 0;
        }
    }
}
