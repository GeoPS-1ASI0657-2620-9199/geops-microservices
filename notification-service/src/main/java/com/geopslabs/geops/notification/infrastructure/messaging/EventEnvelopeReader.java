package com.geopslabs.geops.notification.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.UUID;

/**
 * Reads the envelope once for every listener. It is a tolerant reader: it takes the fields it needs and ignores
 * the ones it does not know, so an optional field added by the publisher does not stop the consumer. What it
 * cannot read, or a version newer than the one the consumer knows, is an {@link UnreadableEventException}: it goes
 * to the dead letter topic at once and is never retried.
 */
@Component
public class EventEnvelopeReader {
    private static final String PAYLOAD = "payload";

    private final ObjectMapper objectMapper;

    public EventEnvelopeReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EventEnvelope read(String value) {
        var node = parse(value);
        var eventType = node.path("eventType");
        var version = node.path("version");
        var aggregateId = node.path("aggregateId");
        var payload = node.path(PAYLOAD);
        if (!eventType.isTextual() || eventType.asText().isBlank() || !version.canConvertToInt()
                || !aggregateId.canConvertToLong() || !payload.isObject()) {
            throw new UnreadableEventException("The envelope lacks eventType, version, aggregateId or payload");
        }
        try {
            var correlation = node.path("correlationId");
            return new EventEnvelope(UUID.fromString(node.path("eventId").asText()), eventType.asText(),
                    version.asInt(), Instant.parse(node.path("occurredAt").asText()), aggregateId.asLong(),
                    correlation.isTextual() ? correlation.asText() : null, payload);
        } catch (IllegalArgumentException | DateTimeException exception) {
            throw new UnreadableEventException("The envelope has an invalid eventId or occurredAt", exception);
        }
    }

    /** A version newer than the one this consumer understands is not guessed: it is treated as unreadable. */
    public EventEnvelope requireVersion(EventEnvelope envelope, int maxVersion) {
        if (envelope.version() > maxVersion) {
            throw new UnreadableEventException("%s version %d is newer than the supported %d"
                    .formatted(envelope.eventType(), envelope.version(), maxVersion));
        }
        return envelope;
    }

    private JsonNode parse(String value) {
        try {
            var node = objectMapper.readTree(value);
            if (node == null || !node.isObject()) {
                throw new UnreadableEventException("The record is not a JSON object");
            }
            return node;
        } catch (JsonProcessingException exception) {
            throw new UnreadableEventException("The record is not valid JSON", exception);
        }
    }
}
