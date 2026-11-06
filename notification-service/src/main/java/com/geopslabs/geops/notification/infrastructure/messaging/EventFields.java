package com.geopslabs.geops.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
public final class EventFields {
    private EventFields() {}
    public static Long id(JsonNode data, String name) {
        if (!data.path(name).isIntegralNumber() || !data.path(name).canConvertToLong() || data.path(name).asLong() <= 0) throw new UnreadableEventException("Invalid " + name);
        return data.path(name).asLong();
    }
    public static String text(JsonNode data, String name) {
        if (!data.path(name).isTextual() || data.path(name).asText().isBlank()) throw new UnreadableEventException("Invalid " + name);
        return data.path(name).asText();
    }
}
