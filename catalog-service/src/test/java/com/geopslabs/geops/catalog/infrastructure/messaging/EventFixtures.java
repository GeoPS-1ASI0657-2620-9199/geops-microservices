package com.geopslabs.geops.catalog.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/**
 * Reads the examples of BRD-11 that the service copied to src/test/resources/events/ and lets a test change the
 * eventId or one field of the payload, so the producer and the consumer are tested against the same files.
 */
public final class EventFixtures {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EventFixtures() {
    }

    public static String read(String eventType) {
        return text(node(eventType));
    }

    public static String withEventId(String eventType, UUID eventId) {
        var node = node(eventType);
        node.put("eventId", eventId.toString());
        return text(node);
    }

    public static String withPayloadField(String eventType, String field, Object value) {
        var node = node(eventType);
        ((ObjectNode) node.get("payload")).set(field, MAPPER.valueToTree(value));
        return text(node);
    }

    private static ObjectNode node(String eventType) {
        var path = "/events/" + eventType + ".v1.json";
        try (var in = EventFixtures.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Copy " + eventType + ".v1.json to src/test/resources/events/");
            }
            return (ObjectNode) MAPPER.readTree(in);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String text(ObjectNode node) {
        try {
            return MAPPER.writeValueAsString(node);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
