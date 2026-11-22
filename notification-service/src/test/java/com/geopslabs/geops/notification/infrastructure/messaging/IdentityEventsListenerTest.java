package com.geopslabs.geops.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.notification.application.usecases.ApplyIdentityEventsUseCase;
import com.geopslabs.geops.notification.application.usecases.SendEmailConfirmationUseCase;
import com.geopslabs.geops.notification.domain.models.ConfirmationLink;
import com.geopslabs.geops.notification.domain.models.Recipient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** US28: what Notification does with each event of identity.events, read from the examples of the contract. */
class IdentityEventsListenerTest {
    private static final String TOPIC = "identity.events";

    private final ApplyIdentityEventsUseCase projector = mock(ApplyIdentityEventsUseCase.class);
    private final SendEmailConfirmationUseCase confirmations = mock(SendEmailConfirmationUseCase.class);
    private final Acknowledgment ack = mock(Acknowledgment.class);
    private final IdentityEventsListener listener = new IdentityEventsListener(
            new EventEnvelopeReader(new ObjectMapper()), projector, confirmations);

    @Test
    void userRegisteredCreatesTheRecipientAndSendsTheConfirmationLink() {
        listener.onEvent(record(EventFixtures.read("UserRegistered")), ack);

        verify(projector).userRegistered(new Recipient(2001L, "lucia.fernandez@ejemplo.pe", false, "CONSUMER", null),
                Instant.parse("2026-10-27T14:00:00Z"));
        verify(confirmations).send(new ConfirmationLink(2001L, "lucia.fernandez@ejemplo.pe",
                "q3Zt8b1xV0mWc9yRkP2sLhN4dF6gJ7aE5uT0iO3pQ1w", Instant.parse("2026-10-28T14:00:00Z")));
        verify(ack).acknowledge();
    }

    @Test
    void aUserRegisteredWithoutLinkOnlyCreatesTheRecipient() {
        listener.onEvent(record(EventFixtures.withPayloadField("UserRegistered", "confirmationToken", null)), ack);

        verify(projector).userRegistered(any(), any());
        verifyNoInteractions(confirmations);
    }

    @Test
    void anAlreadyConfirmedEmailGetsNoConfirmationEmail() {
        listener.onEvent(record(EventFixtures.withPayloadField("UserRegistered", "emailConfirmed", true)), ack);

        verifyNoInteractions(confirmations);
    }

    @Test
    void emailConfirmedMarksTheRecipientCopy() {
        listener.onEvent(record(EventFixtures.read("EmailConfirmed")), ack);

        verify(projector).emailConfirmed(2001L, "lucia.fernandez@ejemplo.pe");
        verify(ack).acknowledge();
    }

    @Test
    void anUnreadableExpiryGoesToTheDeadLetterTopic() {
        var value = EventFixtures.withPayloadField("UserRegistered", "confirmationExpiresAt", "mañana");

        assertThatThrownBy(() -> listener.onEvent(record(value), ack)).isInstanceOf(UnreadableEventException.class);
        verify(ack, never()).acknowledge();
    }

    @Test
    void aNewerVersionOfEmailConfirmedIsNotGuessed() {
        var value = EventFixtures.read("EmailConfirmed").replace("\"version\":1", "\"version\":2");

        assertThatThrownBy(() -> listener.onEvent(record(value), ack)).isInstanceOf(UnreadableEventException.class);
        verifyNoInteractions(projector);
    }

    private static ConsumerRecord<String, String> record(String value) {
        return new ConsumerRecord<>(TOPIC, 0, 0L, "2001", value);
    }
}
