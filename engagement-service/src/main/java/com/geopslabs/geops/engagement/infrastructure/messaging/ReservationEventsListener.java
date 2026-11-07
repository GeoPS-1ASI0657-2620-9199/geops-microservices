package com.geopslabs.geops.engagement.infrastructure.messaging;

import com.geopslabs.geops.engagement.application.usecases.ProjectRedemptionUseCase;
import com.geopslabs.geops.engagement.domain.models.RedeemedReservation;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import java.time.*;

@Component
public class ReservationEventsListener {
    private static final int VERSION = 1;
    private final EventEnvelopeReader reader;
    private final ProjectRedemptionUseCase projector;
    public ReservationEventsListener(EventEnvelopeReader reader, ProjectRedemptionUseCase projector) { this.reader = reader; this.projector = projector; }

    @KafkaListener(id = "engagement-reservation-events", topics = "${geops.events.reservation-topic:reservation.events}",
            groupId = "engagement-service", autoStartup = "${geops.messaging.enabled:true}")
    public void onEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var event = reader.read(record.value());
        if ("ReservationRedeemed".equals(event.eventType())) {
            projector.reservationRedeemed(redemption(reader.requireVersion(event, VERSION)));
        }
        ack.acknowledge();
    }

    private RedeemedReservation redemption(EventEnvelope event) {
        var data = event.payload();
        try {
            return new RedeemedReservation(CatalogEventsListener.id(data, "reservationId"), CatalogEventsListener.id(data, "consumerId"),
                    CatalogEventsListener.id(data, "businessId"), LocalDateTime.ofInstant(Instant.parse(CatalogEventsListener.text(data, "redeemedAt")), ZoneOffset.UTC));
        } catch (DateTimeException error) { throw new UnreadableEventException("Invalid redeemedAt", error); }
    }
}
