package com.geopslabs.geops.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventEnvelopeReaderTest {
    private static final String VALID = """
            {"eventId":"8e0f1c64-2a51-4f0b-9b8e-6f1d1c2a7e10","eventType":"OfferPublished","version":1,
             "occurredAt":"2026-10-27T15:04:05Z","aggregateId":77,"payload":{"offerId":9}}""";

    private final EventEnvelopeReader reader = new EventEnvelopeReader(new ObjectMapper());

    @Test
    void readsTheSixFieldsAndLeavesTheCorrelationEmptyWhenThePublisherHadNone() {
        var envelope = reader.read(VALID);

        assertThat(envelope.eventType()).isEqualTo("OfferPublished");
        assertThat(envelope.version()).isEqualTo(1);
        assertThat(envelope.aggregateId()).isEqualTo(77L);
        assertThat(envelope.occurredAt()).hasToString("2026-10-27T15:04:05Z");
        assertThat(envelope.correlationId()).isNull();
        assertThat(envelope.payload().path("offerId").asLong()).isEqualTo(9L);
    }

    @Test
    void readsTheOptionalCorrelationIdAndIgnoresFieldsItDoesNotKnow() {
        var envelope = reader.read(VALID.replace("\"version\":1,", "\"version\":1,\"correlationId\":\"req-42\",\"newField\":true,"));

        assertThat(envelope.correlationId()).isEqualTo("req-42");
    }

    @Test
    void aVersionNewerThanTheKnownOneIsUnreadable() {
        var envelope = reader.read(VALID.replace("\"version\":1", "\"version\":2"));

        assertThatThrownBy(() -> reader.requireVersion(envelope, 1)).isInstanceOf(UnreadableEventException.class)
                .hasMessageContaining("version 2");
        assertThat(reader.requireVersion(reader.read(VALID), 1)).isNotNull();
    }

    @Test
    void whatCannotBeReadIsUnreadable() {
        assertThatThrownBy(() -> reader.read("no-json")).isInstanceOf(UnreadableEventException.class);
        assertThatThrownBy(() -> reader.read("[1,2]")).isInstanceOf(UnreadableEventException.class);
        assertThatThrownBy(() -> reader.read(VALID.replace("\"payload\":{\"offerId\":9}", "\"payload\":3")))
                .isInstanceOf(UnreadableEventException.class);
        assertThatThrownBy(() -> reader.read(VALID.replace("8e0f1c64-2a51-4f0b-9b8e-6f1d1c2a7e10", "nope")))
                .isInstanceOf(UnreadableEventException.class);
        assertThatThrownBy(() -> reader.read(VALID.replace("2026-10-27T15:04:05Z", "ayer")))
                .isInstanceOf(UnreadableEventException.class);
        assertThatThrownBy(() -> reader.read(VALID.replace("\"eventType\":\"OfferPublished\",", "")))
                .isInstanceOf(UnreadableEventException.class);
    }
}
