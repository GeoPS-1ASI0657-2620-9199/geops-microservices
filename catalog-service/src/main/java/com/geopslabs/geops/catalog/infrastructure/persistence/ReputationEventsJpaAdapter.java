package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.ports.ReputationEventsPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * processed_events and standing_changes with plain inserts. ON CONFLICT DO NOTHING makes the check of a repeated
 * event atomic: two deliveries of the same event in parallel insert one row, and only one of them goes on.
 */
@Component
public class ReputationEventsJpaAdapter implements ReputationEventsPort {
    private static final String MARK_PROCESSED = """
            INSERT INTO processed_events (event_id, event_type, processed_at)
            VALUES (:eventId, :eventType, :processedAt)
            ON CONFLICT (event_id) DO NOTHING
            """;
    private static final String RECORD_SEAL_CHANGE = """
            INSERT INTO standing_changes (business_id, verified_seal, reason, changed_at)
            VALUES (:businessId, :verifiedSeal, :reason, :changedAt)
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean markProcessed(UUID eventId, String eventType, Instant processedAt) {
        return entityManager.createNativeQuery(MARK_PROCESSED)
                .setParameter("eventId", eventId)
                .setParameter("eventType", eventType)
                .setParameter("processedAt", Timestamp.from(processedAt))
                .executeUpdate() == 1;
    }

    @Override
    public void recordSealChange(Long businessId, boolean verifiedSeal, String reason, Instant changedAt) {
        entityManager.createNativeQuery(RECORD_SEAL_CHANGE)
                .setParameter("businessId", businessId)
                .setParameter("verifiedSeal", verifiedSeal)
                .setParameter("reason", reason)
                .setParameter("changedAt", Timestamp.from(changedAt))
                .executeUpdate();
    }
}
