package com.geopslabs.geops.reservation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    /**
     * Oldest PENDING rows, locked for this transaction. FAILED rows are left out, so they no longer hold back
     * the ones behind them.
     */
    @Query(value = """
            SELECT * FROM outbox_events
             WHERE status = 'PENDING'
             ORDER BY created_at
             LIMIT :limit
               FOR UPDATE SKIP LOCKED""", nativeQuery = true)
    List<OutboxEventJpaEntity> lockPending(@Param("limit") int limit);

    List<OutboxEventJpaEntity> findByAggregateIdAndEventType(Long aggregateId, String eventType);
}
