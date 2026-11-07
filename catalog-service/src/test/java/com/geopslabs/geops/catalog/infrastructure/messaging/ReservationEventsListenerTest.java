package com.geopslabs.geops.catalog.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.geopslabs.geops.catalog.application.usecases.ApplyComplianceEventUseCase;
import com.geopslabs.geops.catalog.application.usecases.ComplianceEvent;
import com.geopslabs.geops.catalog.domain.models.ComplianceChange;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The listener reads the examples of BRD-11 (platform/messaging/events) as Reservation writes them.
 */
class ReservationEventsListenerTest {
    private static final String TOPIC = "reservation.events";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ApplyComplianceEventUseCase useCase;
    private Acknowledgment acknowledgment;
    private ReservationEventsListener listener;

    @BeforeEach
    void setUp() {
        useCase = mock(ApplyComplianceEventUseCase.class);
        acknowledgment = mock(Acknowledgment.class);
        listener = new ReservationEventsListener(useCase, new EventEnvelopeReader(MAPPER));
    }

    @Test
    void aRedemptionIsAppliedWithItsBusinessAndThenAcknowledged() {
        listener.onReservationEvent(record(example("ReservationRedeemed")), acknowledgment);

        var event = ArgumentCaptor.forClass(ComplianceEvent.class);
        var order = inOrder(useCase, acknowledgment);
        order.verify(useCase).apply(event.capture());
        order.verify(acknowledgment).acknowledge();
        assertThat(event.getValue().eventId()).isEqualTo(UUID.fromString("5b1f8f0e-8a8f-4c62-9a63-0f5a2e1c7d11"));
        assertThat(event.getValue().eventType()).isEqualTo("ReservationRedeemed");
        assertThat(event.getValue().occurredAt()).isEqualTo(Instant.parse("2026-10-20T17:42:10Z"));
        assertThat(event.getValue().businessId()).isEqualTo(84L);
        assertThat(event.getValue().change()).isEqualTo(ComplianceChange.forRedemption());
    }

    @Test
    void eachReportEventCarriesItsChange() {
        assertThat(changeApplied(example("ReportOpened"))).isEqualTo(ComplianceChange.forReportOpened());
        assertThat(changeApplied(example("ReportAnswered"))).isEqualTo(ComplianceChange.forReportAnswered());
        assertThat(changeApplied(example("ReportResolved")))
                .isEqualTo(ComplianceChange.forReportResolved("OPEN", "CONSUMER_FAVOR"));
    }

    @Test
    void otherEventTypesAreAcknowledgedAndIgnored() {
        var created = example("ReservationRedeemed").put("eventType", "ReservationCreated");

        listener.onReservationEvent(record(created), acknowledgment);

        verifyNoInteractions(useCase);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void aReportResolvedWithoutResolutionCannotBeRead() {
        var resolved = example("ReportResolved");
        ((ObjectNode) resolved.path("payload")).remove("resolution");

        assertThatThrownBy(() -> listener.onReservationEvent(record(resolved), acknowledgment))
                .isInstanceOf(UnreadableEventException.class);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void anEventWithoutBusinessCannotBeRead() {
        var opened = example("ReportOpened");
        ((ObjectNode) opened.path("payload")).remove("businessId");

        assertThatThrownBy(() -> listener.onReservationEvent(record(opened), acknowledgment))
                .isInstanceOf(UnreadableEventException.class);
        verify(useCase, never()).apply(any());
    }

    @Test
    void aNewerVersionThanTheOneCatalogKnowsCannotBeRead() {
        var opened = example("ReportOpened").put("version", 2);

        assertThatThrownBy(() -> listener.onReservationEvent(record(opened), acknowledgment))
                .isInstanceOf(UnreadableEventException.class);
        verifyNoInteractions(useCase);
    }

    private ComplianceChange changeApplied(ObjectNode envelope) {
        useCase = mock(ApplyComplianceEventUseCase.class);
        listener = new ReservationEventsListener(useCase, new EventEnvelopeReader(MAPPER));
        listener.onReservationEvent(record(envelope), acknowledgment);
        var event = ArgumentCaptor.forClass(ComplianceEvent.class);
        verify(useCase).apply(event.capture());
        return event.getValue().change();
    }

    private static ObjectNode example(String eventType) {
        return (ObjectNode) EventContract.example(eventType);
    }

    private static ConsumerRecord<String, String> record(ObjectNode envelope) {
        return new ConsumerRecord<>(TOPIC, 0, 0L, envelope.path("aggregateId").asText(), envelope.toString());
    }
}
