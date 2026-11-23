package com.geopslabs.geops.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.notification.application.usecases.CancelOfferNoticesUseCase;
import com.geopslabs.geops.notification.application.usecases.PlanOfferNoticesUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

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

    private PlanOfferNoticesUseCase planner;
    private CancelOfferNoticesUseCase cancel;
    private Acknowledgment acknowledgment;
    private CatalogEventsListener listener;

    @BeforeEach
    void setUp() {
        planner = mock(PlanOfferNoticesUseCase.class);
        cancel = mock(CancelOfferNoticesUseCase.class);
        acknowledgment = mock(Acknowledgment.class);
        listener = new CatalogEventsListener(new EventEnvelopeReader(new ObjectMapper()), planner, cancel);
    }

    @Test
    void aRemovedOfferCancelsItsPendingNoticesAndThenIsAcknowledged() {
        listener.onEvent(record(EventFixtures.read("OfferRemoved")), acknowledgment);

        var order = inOrder(cancel, acknowledgment);
        order.verify(cancel).offerUnavailable(List.of(1052L));
        order.verify(acknowledgment).acknowledge();
        verifyNoInteractions(planner);
    }

    @Test
    void anExpiredOfferStillCancelsItsPendingNotices() {
        listener.onEvent(record(EventFixtures.read("OfferExpired")), acknowledgment);

        verify(cancel).offerUnavailable(List.of(1052L));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void aRemovedOfferWithAnInvalidOfferIdCannotBeRead() {
        var removed = EventFixtures.withPayloadField("OfferRemoved", "offerId", 0);

        assertThatThrownBy(() -> listener.onEvent(record(removed), acknowledgment))
                .isInstanceOf(UnreadableEventException.class);
        verifyNoInteractions(cancel);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void aNewerVersionOfOfferRemovedCannotBeRead() {
        var removed = EventFixtures.read("OfferRemoved").replace("\"version\":1", "\"version\":2");

        assertThatThrownBy(() -> listener.onEvent(record(removed), acknowledgment))
                .isInstanceOf(UnreadableEventException.class);
        verifyNoInteractions(cancel);
    }

    private static ConsumerRecord<String, String> record(String value) {
        return new ConsumerRecord<>(TOPIC, 0, 0L, "77", value);
    }
}
