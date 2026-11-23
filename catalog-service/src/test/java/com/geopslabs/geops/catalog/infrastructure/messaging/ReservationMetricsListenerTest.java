package com.geopslabs.geops.catalog.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.geopslabs.geops.catalog.application.usecases.ApplyReservationMetricEventUseCase;
import com.geopslabs.geops.catalog.application.usecases.ReservationMetricEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** The metrics consumer reads ReservationCreated and ReservationRedeemed as Reservation writes them (BRD-11). */
class ReservationMetricsListenerTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ApplyReservationMetricEventUseCase useCase;
    private Acknowledgment acknowledgment;
    private ReservationMetricsListener listener;

    @BeforeEach
    void setUp() {
        useCase = mock(ApplyReservationMetricEventUseCase.class);
        acknowledgment = mock(Acknowledgment.class);
        listener = new ReservationMetricsListener(useCase, new EventEnvelopeReader(MAPPER));
    }

    @Test
    void aCreatedReservationIsCountedWithItsOfferAndThenAcknowledged() {
        listener.onReservationEvent(record(example("ReservationCreated")), acknowledgment);

        var event = ArgumentCaptor.forClass(ReservationMetricEvent.class);
        var order = inOrder(useCase, acknowledgment);
        order.verify(useCase).apply(event.capture());
        order.verify(acknowledgment).acknowledge();
        assertThat(event.getValue()).isEqualTo(new ReservationMetricEvent(
                UUID.fromString("3c2a7d4e-91b0-4f6e-8d25-6a1f0b9c4e72"), "ReservationCreated",
                ReservationMetricEvent.Kind.RESERVED, 1052L, Instant.parse("2026-10-20T15:05:00Z")));
    }

    @Test
    void aRedemptionIsCountedAsRedeemed() {
        listener.onReservationEvent(record(example("ReservationRedeemed")), acknowledgment);

        var event = ArgumentCaptor.forClass(ReservationMetricEvent.class);
        verify(useCase).apply(event.capture());
        assertThat(event.getValue().kind()).isEqualTo(ReservationMetricEvent.Kind.REDEEMED);
        assertThat(event.getValue().offerId()).isEqualTo(1052L);
    }

    @Test
    void reportEventsAreAcknowledgedAndIgnored() {
        listener.onReservationEvent(record(example("ReportOpened")), acknowledgment);

        verifyNoInteractions(useCase);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void anEventWithoutOfferCannotBeRead() {
        var created = example("ReservationCreated");
        ((ObjectNode) created.path("payload")).remove("offerId");

        assertThatThrownBy(() -> listener.onReservationEvent(record(created), acknowledgment))
                .isInstanceOf(UnreadableEventException.class);
        verifyNoInteractions(useCase);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void aNewerVersionCannotBeRead() {
        var created = example("ReservationCreated").put("version", 2);

        assertThatThrownBy(() -> listener.onReservationEvent(record(created), acknowledgment))
                .isInstanceOf(UnreadableEventException.class);
        verifyNoInteractions(useCase);
    }

    private static ObjectNode example(String eventType) {
        return (ObjectNode) EventContract.example(eventType);
    }

    private static ConsumerRecord<String, String> record(ObjectNode envelope) {
        return new ConsumerRecord<>("reservation.events", 0, 0L, envelope.path("aggregateId").asText(),
                envelope.toString());
    }
}
