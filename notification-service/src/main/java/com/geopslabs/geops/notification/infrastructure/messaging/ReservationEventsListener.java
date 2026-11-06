package com.geopslabs.geops.notification.infrastructure.messaging;

import com.geopslabs.geops.notification.application.usecases.NotifyReportPartiesUseCase;
import com.geopslabs.geops.notification.domain.models.ReportNoticeEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class ReservationEventsListener {
    private static final int VERSION = 1;
    private static final Set<String> TYPES = Set.of("ReportOpened", "ReportAnswered", "ReportResolved");
    private final EventEnvelopeReader reader;
    private final NotifyReportPartiesUseCase notices;
    public ReservationEventsListener(EventEnvelopeReader reader, NotifyReportPartiesUseCase notices) { this.reader = reader; this.notices = notices; }
    @KafkaListener(id = "notification-reservation-events", topics = "${geops.events.reservation-topic:reservation.events}", groupId = "notification-service", autoStartup = "${geops.messaging.enabled:true}")
    public void onEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var event = reader.read(record.value());
        if (TYPES.contains(event.eventType())) apply(reader.requireVersion(event, VERSION));
        ack.acknowledge();
    }
    private void apply(EventEnvelope event) {
        var p = event.payload();
        if ("ReportResolved".equals(event.eventType()) && !Set.of("CONSUMER_FAVOR", "BUSINESS_FAVOR").contains(EventFields.text(p, "resolution"))) throw new UnreadableEventException("Invalid report resolution");
        notices.apply(new ReportNoticeEvent(event.eventType(), EventFields.id(p, "reportId"), EventFields.id(p, "consumerId"), EventFields.id(p, "businessId"), EventFields.text(p, "reservationCode"), EventFields.text(p, "offerTitle"), p.path("resolution").asText(null)));
    }
}
