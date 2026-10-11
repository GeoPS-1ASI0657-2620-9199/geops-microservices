package com.geopslabs.geops.identity.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.identity.domain.events.DomainEvent;
import com.geopslabs.geops.identity.domain.ports.OutboxPort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.LinkedHashMap;

/**
 * Writes the envelope of E2-02 into outbox_events. Runs inside the transaction of the use case that calls it.
 */
@Component
public class OutboxJpaAdapter implements OutboxPort {
    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxJpaAdapter(OutboxEventJpaRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void append(DomainEvent event) {
        var entity = new OutboxEventJpaEntity();
        entity.setId(event.eventId());
        entity.setAggregateId(event.aggregateId());
        entity.setEventType(event.eventType());
        entity.setPayload(envelopeOf(event));
        entity.setCreatedAt(clock.instant());
        repository.save(entity);
    }

    private String envelopeOf(DomainEvent event) {
        var envelope = new LinkedHashMap<String, Object>();
        envelope.put("eventId", event.eventId());
        envelope.put("eventType", event.eventType());
        envelope.put("version", event.version());
        envelope.put("occurredAt", event.occurredAt());
        envelope.put("aggregateId", event.aggregateId());
        envelope.put("payload", event.payload());
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Event " + event.eventId() + " could not be serialized", exception);
        }
    }
}
