package com.geopslabs.geops.reservation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
public class OutboxEventJpaEntity {
    private static final int EVENT_TYPE_LENGTH = 80;
    private static final int STATUS_LENGTH = 20;
    private static final int LAST_ERROR_LENGTH = 500;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Assigned by the database on insert; the relay publishes in this order. */
    @Column(name = "seq", insertable = false, updatable = false)
    private Long seq;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = EVENT_TYPE_LENGTH)
    private String eventType;

    /** The whole envelope of the event, sent to Kafka as is. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = STATUS_LENGTH)
    private OutboxEventStatus status = OutboxEventStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", length = LAST_ERROR_LENGTH)
    private String lastError;

    public void markPublished(Instant at) {
        publishedAt = at;
        status = OutboxEventStatus.PUBLISHED;
    }

    /** Counts a failed send and keeps its reason, cut to the size of the column. */
    public void recordFailure(String reason) {
        attempts++;
        lastError = reason == null || reason.length() <= LAST_ERROR_LENGTH
                ? reason : reason.substring(0, LAST_ERROR_LENGTH);
    }

    public void markFailed() {
        status = OutboxEventStatus.FAILED;
    }
}
