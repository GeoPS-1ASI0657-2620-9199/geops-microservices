package com.geopslabs.geops.notification.infrastructure.messaging;

import com.geopslabs.geops.notification.application.usecases.ApplyIdentityEventsUseCase;
import com.geopslabs.geops.notification.application.usecases.SendEmailConfirmationUseCase;
import com.geopslabs.geops.notification.domain.models.ConfirmationLink;
import com.geopslabs.geops.notification.domain.models.Recipient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Instant;

/**
 * Notification learns about the accounts of Identity only from identity.events (US28): UserRegistered creates the
 * recipient copy and sends the confirmation email, EmailConfirmed marks the copy as confirmed. There is no call to
 * Identity, so either service can be deployed or be down without the other.
 */
@Component
public class IdentityEventsListener {
    private static final int VERSION = 1;
    private static final String SEARCH_RADIUS = "searchRadiusMinutes";
    private static final String PUBLISHED_RADIUS = "radiusMinutes";
    private final EventEnvelopeReader reader;
    private final ApplyIdentityEventsUseCase projector;
    private final SendEmailConfirmationUseCase confirmations;
    public IdentityEventsListener(EventEnvelopeReader reader, ApplyIdentityEventsUseCase projector, SendEmailConfirmationUseCase confirmations) {
        this.reader = reader; this.projector = projector; this.confirmations = confirmations;
    }

    @KafkaListener(id = "notification-identity-events", topics = "${geops.events.identity-topic:identity.events}", groupId = "notification-service", autoStartup = "${geops.messaging.enabled:true}")
    public void onEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var event = reader.read(record.value());
        switch (event.eventType()) {
            case "UserRegistered" -> registered(reader.requireVersion(event, VERSION));
            case "ConsumerPreferencesChanged" -> radius(reader.requireVersion(event, VERSION));
            case "EmailConfirmed" -> confirmed(reader.requireVersion(event, VERSION));
            default -> { }
        }
        ack.acknowledge();
    }

    private void registered(EventEnvelope event) {
        var p = event.payload();
        if (!p.path("emailConfirmed").isBoolean()) throw new UnreadableEventException("Invalid emailConfirmed");
        var businessId = p.path("businessId").isNull() ? null : EventFields.id(p, "businessId");
        var recipient = new Recipient(EventFields.id(p, "userId"), EventFields.text(p, "email"), p.path("emailConfirmed").asBoolean(), EventFields.text(p, "role"), businessId);
        projector.userRegistered(recipient, event.occurredAt());
        // The link is optional: an account registered before US28, or already confirmed, has none.
        if (!recipient.emailConfirmed() && p.path("confirmationToken").isTextual()) {
            confirmations.send(new ConfirmationLink(recipient.userId(), recipient.email(), EventFields.text(p, "confirmationToken"), instant(p.path("confirmationExpiresAt").asText())));
        }
    }

    private void confirmed(EventEnvelope event) {
        projector.emailConfirmed(EventFields.id(event.payload(), "userId"), EventFields.text(event.payload(), "email"));
    }

    private static Instant instant(String value) {
        try { return Instant.parse(value); } catch (DateTimeException error) { throw new UnreadableEventException("Invalid confirmationExpiresAt", error); }
    }

    /** Identity writes radiusMinutes while the contract says searchRadiusMinutes; both are read until they agree. */
    private void radius(EventEnvelope event) {
        var field = event.payload().has(SEARCH_RADIUS) ? SEARCH_RADIUS : PUBLISHED_RADIUS;
        var minutes = EventFields.id(event.payload(), field);
        if (minutes > 20) throw new UnreadableEventException("Invalid searchRadiusMinutes");
        projector.consumerPreferencesChanged(EventFields.id(event.payload(), "consumerId"), minutes.intValue());
    }
}
