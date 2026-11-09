package com.geopslabs.geops.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    /**
     * Oldest PENDING rows by seq, locked for this transaction. FAILED rows are left out, so they no longer hold back
     * the ones behind them.
     */
    @Query(value = """
            SELECT * FROM outbox_events
             WHERE status = 'PENDING'
             ORDER BY seq
             LIMIT :limit
               FOR UPDATE SKIP LOCKED""", nativeQuery = true)
    List<OutboxEventJpaEntity> lockPending(@Param("limit") int limit);

    /**
     * Transaction-scoped advisory lock: only one relay replica works per round, so the events leave in seq order
     * even with two replicas. Returns false at once when another replica holds it.
     */
    @Query(value = "SELECT pg_try_advisory_xact_lock(hashtext(:name))", nativeQuery = true)
    boolean tryAdvisoryLock(@Param("name") String name);

    List<OutboxEventJpaEntity> findByAggregateIdAndEventType(Long aggregateId, String eventType);
}
