package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.domain.models.Notification;
import com.geopslabs.geops.notification.domain.models.queries.GetNotificationByIdQuery;
import com.geopslabs.geops.notification.domain.models.queries.GetNotificationsByUserIdQuery;
import com.geopslabs.geops.notification.domain.models.queries.GetUnreadCountByUserIdQuery;
import com.geopslabs.geops.notification.application.usecases.NotificationQueryUseCase;
import com.geopslabs.geops.notification.domain.ports.NotificationRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Notification Query Service Implementation
 *
 * Implementation of notification query service operations
 *
 * @summary Implementation of notification query operations
 * @since 1.0
 * @author GeOps Labs
 */
@Transactional(readOnly = true)
public class NotificationQueryService implements NotificationQueryUseCase {

    private final NotificationRepositoryPort notificationRepository;

    public NotificationQueryService(NotificationRepositoryPort notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<Notification> handle(GetNotificationsByUserIdQuery query) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(query.userId());
    }

    @Override
    public Optional<Notification> handle(GetNotificationByIdQuery query) {
        return notificationRepository.findById(query.notificationId());
    }

    @Override
    public Long handle(GetUnreadCountByUserIdQuery query) {
        return notificationRepository.countByUserIdAndIsRead(query.userId(), false);
    }
}
