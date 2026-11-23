package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.ports.ProcessedEventsPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * processed_events by consumer. ON CONFLICT DO NOTHING makes the check atomic, as in ReputationEventsJpaAdapter:
 * two deliveries of the same event in parallel insert one row and only one of them goes on.
 */
@Component
public class ProcessedEventsJdbcAdapter implements ProcessedEventsPort {
    private static final String MARK_PROCESSED = """
            INSERT INTO processed_events (event_id, event_type, consumer, processed_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (event_id, consumer) DO NOTHING
            """;

    private final JdbcTemplate jdbc;

    public ProcessedEventsJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean markProcessed(UUID eventId, String eventType, String consumer, Instant processedAt) {
        return jdbc.update(MARK_PROCESSED, eventId, eventType, consumer, Timestamp.from(processedAt)) == 1;
    }
}
