package com.geopslabs.geops.notification.infrastructure.messaging;

import com.geopslabs.geops.notification.application.usecases.ApplyFollowEventsUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class EngagementEventsListener {
    private static final int VERSION = 1;
    private final EventEnvelopeReader reader;
    private final ApplyFollowEventsUseCase projector;
    public EngagementEventsListener(EventEnvelopeReader reader, ApplyFollowEventsUseCase projector) { this.reader = reader; this.projector = projector; }
    @KafkaListener(id = "notification-engagement-events", topics = "${geops.events.engagement-topic:engagement.events}", groupId = "notification-service", autoStartup = "${geops.messaging.enabled:true}")
    public void onEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var event = reader.read(record.value());
        if ("BusinessFollowed".equals(event.eventType()) || "BusinessUnfollowed".equals(event.eventType())) apply(reader.requireVersion(event, VERSION));
        ack.acknowledge();
    }
    private void apply(EventEnvelope event) {
        var consumerId = EventFields.id(event.payload(), "consumerId");
        var businessId = EventFields.id(event.payload(), "businessId");
        if ("BusinessFollowed".equals(event.eventType())) projector.businessFollowed(consumerId, businessId);
        else projector.businessUnfollowed(consumerId, businessId);
    }
}
