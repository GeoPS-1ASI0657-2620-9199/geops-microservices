package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.domain.models.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {

    long countByRecipientIdAndStatusAndSentAtGreaterThanEqual(Long recipientId, DeliveryStatus status,
                                                              Instant since);
}
