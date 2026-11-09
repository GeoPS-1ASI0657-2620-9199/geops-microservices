package com.geopslabs.geops.catalog.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.catalog.application.usecases.ApplyBusinessVerifiedUseCase;
import com.geopslabs.geops.catalog.application.usecases.BusinessVerifiedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class IdentityEventsListenerTest {
    private static final String BUSINESS_VERIFIED = """
            {"eventId": "7d1f8f5e-2b8c-4a7e-9a40-2f1d2a6c9b11", "eventType": "BusinessVerified", "version": 1,
             "occurredAt": "2026-10-06T15:00:00Z", "aggregateId": 84,
             "payload": {"businessId": 84, "businessName": "Restaurante Don Pepe", "verified": true,
                         "result": "APPROVED", "reason": null, "verifiedAt": "2026-10-06T15:00:00Z"}}""";

    private ApplyBusinessVerifiedUseCase useCase;
    private Acknowledgment acknowledgment;
    private IdentityEventsListener listener;

    @BeforeEach
    void setUp() {
        useCase = mock(ApplyBusinessVerifiedUseCase.class);
        acknowledgment = mock(Acknowledgment.class);
        listener = new IdentityEventsListener(useCase, new ObjectMapper());
    }

    @Test
    void readsTheEnvelopeIdentityWritesAndAcknowledgesAfterApplyingIt() throws Exception {
        listener.onIdentityEvent(record(BUSINESS_VERIFIED), acknowledgment);

        var event = ArgumentCaptor.forClass(BusinessVerifiedEvent.class);
        var order = org.mockito.Mockito.inOrder(useCase, acknowledgment);
        order.verify(useCase).apply(event.capture());
        order.verify(acknowledgment).acknowledge();
        assertThat(event.getValue().eventId()).isEqualTo(UUID.fromString("7d1f8f5e-2b8c-4a7e-9a40-2f1d2a6c9b11"));
        assertThat(event.getValue().businessId()).isEqualTo(84L);
        assertThat(event.getValue().businessName()).isEqualTo("Restaurante Don Pepe");
        assertThat(event.getValue().verified()).isTrue();
        assertThat(event.getValue().occurredAt()).isEqualTo(Instant.parse("2026-10-06T15:00:00Z"));
    }

    @Test
    void otherEventTypesAreAcknowledgedAndIgnored() throws Exception {
        listener.onIdentityEvent(record("""
                {"eventId": "1b0c8a2e-0000-4000-8000-000000000001", "eventType": "ConsumerPreferencesChanged",
                 "payload": {}}"""), acknowledgment);

        verifyNoInteractions(useCase);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void anEventWithoutBusinessIsNotAcknowledgedSoTheErrorHandlerTakesIt() {
        assertThatThrownBy(() -> listener.onIdentityEvent(record("""
                {"eventId": "1b0c8a2e-0000-4000-8000-000000000002", "eventType": "BusinessVerified",
                 "occurredAt": "2026-10-06T15:00:00Z", "payload": {"verified": true}}"""), acknowledgment))
                .isInstanceOf(UnreadableEventException.class);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void brokenJsonAndAnInvalidEventIdAreUnreadable() {
        assertThatThrownBy(() -> listener.onIdentityEvent(record("{not json"), acknowledgment))
                .isInstanceOf(UnreadableEventException.class);
        assertThatThrownBy(() -> listener.onIdentityEvent(record(BUSINESS_VERIFIED
                .replace("7d1f8f5e-2b8c-4a7e-9a40-2f1d2a6c9b11", "not-a-uuid")), acknowledgment))
                .isInstanceOf(UnreadableEventException.class);
        assertThatThrownBy(() -> listener.onIdentityEvent(record(BUSINESS_VERIFIED
                .replace("\"occurredAt\": \"2026-10-06T15:00:00Z\"", "\"occurredAt\": \"ayer\"")), acknowledgment))
                .isInstanceOf(UnreadableEventException.class);
        verifyNoInteractions(useCase);
    }

    @Test
    void aFailureWhileApplyingTheEventIsNotTheEventsFault() {
        org.mockito.Mockito.when(useCase.apply(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new NullPointerException("bug"));

        assertThatThrownBy(() -> listener.onIdentityEvent(record(BUSINESS_VERIFIED), acknowledgment))
                .isExactlyInstanceOf(NullPointerException.class);
        verify(acknowledgment, never()).acknowledge();
    }

    private static ConsumerRecord<String, String> record(String value) {
        return new ConsumerRecord<>("identity.events", 0, 0L, "84", value);
    }
}
