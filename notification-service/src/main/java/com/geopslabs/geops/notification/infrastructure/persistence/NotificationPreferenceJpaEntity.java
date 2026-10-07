package com.geopslabs.geops.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
public class NotificationPreferenceJpaEntity {

    @Id
    @Column(name = "consumer_id")
    private Long consumerId;

    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled;

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "daily_limit", nullable = false)
    private Integer dailyLimit;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
