package com.geopslabs.geops.reservation.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The contract of an event is the JSON example of BRD-11 (platform/messaging/events). A publisher passes if what it
 * writes has the same field names and the same kinds of value, not the same values; a null in the example or in the
 * event matches any kind, because an optional field is null in some cases. A field added, removed or renamed
 * without updating the example fails here.
 */
public final class EventContract {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EventContract() {
    }

    public static JsonNode example(String eventType) {
        try (var in = EventContract.class.getResourceAsStream("/events/" + eventType + ".v1.json")) {
            assertThat(in).as("src/test/resources/events/%s.v1.json", eventType).isNotNull();
            return MAPPER.readTree(in);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static void assertSameShape(String actualJson, String eventType) {
        JsonNode actual;
        try {
            actual = MAPPER.readTree(actualJson);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        var differences = new ArrayList<String>();
        compare("", actual, example(eventType), differences);
        assertThat(differences).as("%s differs from its example in BRD-11", eventType).isEmpty();
    }

    private static void compare(String path, JsonNode actual, JsonNode expected, List<String> differences) {
        if (actual.isNull() || expected.isNull()) {
            return;
        }
        if (actual.getNodeType() != expected.getNodeType()) {
            differences.add(path + ": " + actual.getNodeType() + " instead of " + expected.getNodeType());
            return;
        }
        if (expected.isObject()) {
            expected.fieldNames().forEachRemaining(name -> {
                if (!actual.has(name)) {
                    differences.add(path + "/" + name + " is missing");
                } else {
                    compare(path + "/" + name, actual.get(name), expected.get(name), differences);
                }
            });
            actual.fieldNames().forEachRemaining(name -> {
                if (!expected.has(name)) {
                    differences.add(path + "/" + name + " is not in the example");
                }
            });
        }
    }
}
