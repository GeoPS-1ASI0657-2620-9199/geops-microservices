package com.geopslabs.geops.notification.infrastructure.messaging;

import com.geopslabs.geops.notification.application.usecases.*;
import com.geopslabs.geops.notification.domain.models.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.StreamSupport;

@Component
public class CatalogEventsListener {
    private static final int VERSION = 1;
    private final EventEnvelopeReader reader;
    private final PlanOfferNoticesUseCase planner;
    private final CancelOfferNoticesUseCase cancel;
    public CatalogEventsListener(EventEnvelopeReader reader, PlanOfferNoticesUseCase planner, CancelOfferNoticesUseCase cancel) { this.reader = reader; this.planner = planner; this.cancel = cancel; }
    @KafkaListener(id = "notification-catalog-events", topics = "${geops.events.catalog-topic:catalog.events}", groupId = "notification-service", autoStartup = "${geops.messaging.enabled:true}")
    public void onEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var event = reader.read(record.value());
        switch (event.eventType()) {
            case "OfferPublished" -> planner.offerPublished(offer(reader.requireVersion(event, VERSION)));
            case "OfferExpired" -> cancel.offerUnavailable(List.of(EventFields.id(reader.requireVersion(event, VERSION).payload(), "offerId")));
            case "CampaignPaused" -> cancel.offerUnavailable(offerIds(reader.requireVersion(event, VERSION).payload().path("offerIds")));
            default -> { }
        }
        ack.acknowledge();
    }
    private static List<Long> offerIds(JsonNode node) {
        if (!node.isArray() || StreamSupport.stream(node.spliterator(), false).anyMatch(n -> !n.isIntegralNumber() || n.asLong() <= 0)) throw new UnreadableEventException("Invalid offerIds");
        return StreamSupport.stream(node.spliterator(), false).map(JsonNode::asLong).toList();
    }
    private static OfferPublishedEvent offer(EventEnvelope event) {
        var p = event.payload();
        try {
            var zone = p.path("zone").isObject() ? new OfferPublishedEvent.Zone(point(p.path("zone").path("center")), Math.toIntExact(EventFields.id(p.path("zone"), "reachMeters"))) : null;
            return new OfferPublishedEvent(EventFields.id(p, "offerId"), EventFields.id(p, "businessId"), EventFields.text(p, "businessName"), EventFields.text(p, "title"), LocalDate.parse(EventFields.text(p, "validTo")), point(p.path("location")), zone, event.occurredAt());
        } catch (IllegalArgumentException | java.time.DateTimeException | ArithmeticException error) { throw new UnreadableEventException("Invalid OfferPublished payload", error); }
    }
    private static GeoPoint point(JsonNode p) {
        if (!p.path("latitude").isNumber() || !p.path("longitude").isNumber()) throw new UnreadableEventException("Invalid location");
        return new GeoPoint(p.path("latitude").asDouble(), p.path("longitude").asDouble());
    }
}
