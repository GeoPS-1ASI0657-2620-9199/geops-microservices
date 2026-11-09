package com.geopslabs.geops.catalog.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.catalog.application.usecases.ApplyBusinessVerifiedUseCase;
import com.geopslabs.geops.catalog.application.usecases.BusinessVerifiedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.UUID;

/**
 * Consumer of identity.events (E2-02). Reads the envelope, applies BusinessVerified and acknowledges it only after the
 * standing is committed and its cells are deleted. Other event types are acknowledged and ignored. An event that
 * cannot be read goes to the error handler of MessagingConfiguration (three retries, then logged and skipped, E2-03).
 */
@Component
public class IdentityEventsListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityEventsListener.class);
    private static final String PAYLOAD = "payload";

    private final ApplyBusinessVerifiedUseCase businessVerified;
    private final ObjectMapper objectMapper;

    public IdentityEventsListener(ApplyBusinessVerifiedUseCase businessVerified, ObjectMapper objectMapper) {
        this.businessVerified = businessVerified;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(id = "catalog-identity-events", topics = "${geops.events.identity-topic:identity.events}",
            groupId = "catalog-service", autoStartup = "${geops.messaging.enabled:true}")
    public void onIdentityEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        var envelope = read(record.value());
        var eventType = envelope.path("eventType").asText();
        if (!BusinessVerifiedEvent.EVENT_TYPE.equals(eventType)) {
            LOGGER.debug("identity.event.ignored eventType={} eventId={}", eventType, envelope.path("eventId").asText());
            acknowledgment.acknowledge();
            return;
        }
        businessVerified.apply(toEvent(envelope));
        acknowledgment.acknowledge();
    }

    private JsonNode read(String value) {
        try {
            var envelope = objectMapper.readTree(value);
            if (envelope == null || !envelope.isObject()) {
                throw new UnreadableEventException("The record is not a JSON object");
            }
            return envelope;
        } catch (JsonProcessingException exception) {
            throw new UnreadableEventException("The record is not valid JSON", exception);
        }
    }

    /** Only reading the envelope is wrapped: an error while applying the event is not the event's fault. */
    static BusinessVerifiedEvent toEvent(JsonNode envelope) {
        var payload = envelope.path(PAYLOAD);
        var businessId = payload.path("businessId");
        if (!businessId.canConvertToLong() || !payload.path("verified").isBoolean()) {
            throw new UnreadableEventException("BusinessVerified without businessId or verified");
        }
        try {
            return new BusinessVerifiedEvent(UUID.fromString(envelope.path("eventId").asText()),
                    Instant.parse(envelope.path("occurredAt").asText()), businessId.asLong(),
                    payload.path("businessName").asText(null), payload.path("verified").asBoolean());
        } catch (IllegalArgumentException | DateTimeException | NullPointerException exception) {
            throw new UnreadableEventException("BusinessVerified with an invalid eventId or occurredAt", exception);
        }
    }
}
