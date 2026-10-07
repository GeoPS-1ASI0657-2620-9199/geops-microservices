package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.domain.models.Channel;
import com.geopslabs.geops.notification.domain.models.DeliveryStatus;
import com.geopslabs.geops.notification.domain.models.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class NotificationJpaEntity {
    private static final int TYPE_LENGTH = 40;
    private static final int CHANNEL_LENGTH = 20;
    private static final int TITLE_LENGTH = 200;
    private static final int MESSAGE_LENGTH = 500;
    private static final int RELATED_ENTITY_LENGTH = 50;
    private static final int STATUS_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = TYPE_LENGTH)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = CHANNEL_LENGTH)
    private Channel channel;

    @Column(name = "title", nullable = false, length = TITLE_LENGTH)
    private String title;

    @Column(name = "message", nullable = false, length = MESSAGE_LENGTH)
    private String message;

    @Column(name = "related_entity_id", length = RELATED_ENTITY_LENGTH)
    private String relatedEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = STATUS_LENGTH)
    private DeliveryStatus status;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;
}
