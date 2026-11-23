package com.geopslabs.geops.engagement.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.engagement.application.usecases.ProjectCatalogEventsUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The listener reads the examples of BRD-11 copied from platform/messaging/events, as Catalog writes them.
 */
class CatalogEventsListenerTest {
    private static final String TOPIC = "catalog.events";

    private ProjectCatalogEventsUseCase projector;
    private Acknowledgment acknowledgment;
    private CatalogEventsListener listener;

    @BeforeEach
    void setUp() {
        projector = mock(ProjectCatalogEventsUseCase.class);
        acknowledgment = mock(Acknowledgment.class);
        listener = new CatalogEventsListener(new EventEnvelopeReader(new ObjectMapper()), projector);
    }

    @Test
    void aRemovedOfferLeavesTheSavedOfferAsRemovedAndThenIsAcknowledged() {
        listener.onEvent(record(EventFixtures.read("OfferRemoved")), acknowledgment);

        var order = inOrder(projector, acknowledgment);
        order.verify(projector).offerRemoved(1052L, Instant.parse("2026-11-22T15:04:05Z"));
        order.verify(acknowledgment).acknowledge();
    }

    @Test
    void anExpiredOfferIsStillProjectedAsExpired() {
        listener.onEvent(record(EventFixtures.read("OfferExpired")), acknowledgment);

        verify(projector).offerExpired(1052L, Instant.parse("2026-11-16T05:00:00Z"));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void aRemovedOfferWithAnInvalidOfferIdCannotBeRead() {
        var removed = EventFixtures.withPayloadField("OfferRemoved", "offerId", "1052");

        assertThatThrownBy(() -> listener.onEvent(record(removed), acknowledgment))
                .isInstanceOf(UnreadableEventException.class);
        verifyNoInteractions(projector);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void aNewerVersionOfOfferRemovedCannotBeRead() {
        var removed = EventFixtures.read("OfferRemoved").replace("\"version\":1", "\"version\":2");

        assertThatThrownBy(() -> listener.onEvent(record(removed), acknowledgment))
                .isInstanceOf(UnreadableEventException.class);
        verifyNoInteractions(projector);
    }

    private static ConsumerRecord<String, String> record(String value) {
        return new ConsumerRecord<>(TOPIC, 0, 0L, "77", value);
    }
}
