package com.geopslabs.geops.reservation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    /**
     * Oldest unpublished rows, locked for this transaction. SKIP LOCKED lets a second replica take other rows
     * instead of waiting or publishing the same ones.
     */
    @Query(value = """
            SELECT * FROM outbox_events
             WHERE published_at IS NULL
             ORDER BY created_at
             LIMIT :limit
               FOR UPDATE SKIP LOCKED""", nativeQuery = true)
    List<OutboxEventJpaEntity> lockUnpublished(@Param("limit") int limit);

    List<OutboxEventJpaEntity> findByAggregateIdAndEventType(Long aggregateId, String eventType);
}
