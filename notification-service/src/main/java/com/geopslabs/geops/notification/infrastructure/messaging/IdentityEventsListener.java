package com.geopslabs.geops.notification.infrastructure.messaging;

import com.geopslabs.geops.notification.application.usecases.ApplyIdentityEventsUseCase;
import com.geopslabs.geops.notification.domain.models.Recipient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class IdentityEventsListener {
    private static final int VERSION = 1;
    private final EventEnvelopeReader reader;
    private final ApplyIdentityEventsUseCase projector;
    public IdentityEventsListener(EventEnvelopeReader reader, ApplyIdentityEventsUseCase projector) { this.reader = reader; this.projector = projector; }

    @KafkaListener(id = "notification-identity-events", topics = "${geops.events.identity-topic:identity.events}", groupId = "notification-service", autoStartup = "${geops.messaging.enabled:true}")
    public void onEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var event = reader.read(record.value());
        switch (event.eventType()) {
            case "UserRegistered" -> registered(reader.requireVersion(event, VERSION));
            case "ConsumerPreferencesChanged" -> radius(reader.requireVersion(event, VERSION));
            default -> { }
        }
        ack.acknowledge();
    }

    private void registered(EventEnvelope event) {
        var p = event.payload();
        if (!p.path("emailConfirmed").isBoolean()) throw new UnreadableEventException("Invalid emailConfirmed");
        var businessId = p.path("businessId").isNull() ? null : EventFields.id(p, "businessId");
        projector.userRegistered(new Recipient(EventFields.id(p, "userId"), EventFields.text(p, "email"), p.path("emailConfirmed").asBoolean(), EventFields.text(p, "role"), businessId), event.occurredAt());
    }

    private void radius(EventEnvelope event) {
        var minutes = EventFields.id(event.payload(), "searchRadiusMinutes");
        if (minutes > 20) throw new UnreadableEventException("Invalid searchRadiusMinutes");
        projector.consumerPreferencesChanged(EventFields.id(event.payload(), "consumerId"), minutes.intValue());
    }
}
