package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.backend.identity.domain.model.aggregates.User;
import com.geopslabs.geops.backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.geopslabs.geops.notification.domain.models.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_is_read", columnList = "is_read"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
public class NotificationJpaEntity extends AuditableAbstractAggregateRoot<NotificationJpaEntity> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "related_entity_id", length = 50)
    private String relatedEntityId;

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    @Column(name = "action_url", length = 500)
    private String actionUrl;
}
