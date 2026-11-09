package com.geopslabs.geops.engagement.infrastructure.messaging;

import com.geopslabs.geops.engagement.application.usecases.ProjectCatalogEventsUseCase;
import com.geopslabs.geops.engagement.domain.models.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.stream.StreamSupport;

@Component
public class CatalogEventsListener {
    private static final int VERSION = 1;
    private final EventEnvelopeReader reader;
    private final ProjectCatalogEventsUseCase projector;
    public CatalogEventsListener(EventEnvelopeReader reader, ProjectCatalogEventsUseCase projector) {
        this.reader = reader; this.projector = projector;
    }

    @KafkaListener(id = "engagement-catalog-events", topics = "${geops.events.catalog-topic:catalog.events}",
            groupId = "engagement-service", autoStartup = "${geops.messaging.enabled:true}")
    public void onEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var event = reader.read(record.value());
        switch (event.eventType()) {
            case "OfferPublished" -> publish(reader.requireVersion(event, VERSION));
            case "CampaignPaused" -> projector.campaignPaused(ids(reader.requireVersion(event, VERSION).payload().path("offerIds")), event.occurredAt());
            case "OfferExpired" -> projector.offerExpired(id(reader.requireVersion(event, VERSION).payload(), "offerId"), event.occurredAt());
            default -> { }
        }
        ack.acknowledge();
    }

    private void publish(EventEnvelope event) {
        var data = event.payload();
        OfferSnapshot offer;
        BusinessSnapshot business;
        try {
            business = new BusinessSnapshot(id(data, "businessId"), text(data, "businessName"));
            offer = new OfferSnapshot(id(data, "offerId"), business.businessId(), text(data, "title"),
                    LocalDate.parse(text(data, "validTo")), OfferSnapshot.PUBLISHED);
        } catch (IllegalArgumentException | java.time.DateTimeException error) {
            throw new UnreadableEventException("Invalid OfferPublished payload", error);
        }
        projector.offerPublished(business, offer, event.occurredAt());
    }

    static Long id(JsonNode data, String name) {
        if (!data.path(name).isIntegralNumber() || !data.path(name).canConvertToLong() || data.path(name).asLong() <= 0) throw new UnreadableEventException("Invalid " + name);
        return data.path(name).asLong();
    }

    static String text(JsonNode data, String name) {
        if (!data.path(name).isTextual() || data.path(name).asText().isBlank()) throw new UnreadableEventException("Invalid " + name);
        return data.path(name).asText();
    }

    private static java.util.List<Long> ids(JsonNode data) {
        if (!data.isArray() || StreamSupport.stream(data.spliterator(), false).anyMatch(n -> !n.isIntegralNumber() || n.asLong() <= 0)) throw new UnreadableEventException("Invalid offerIds");
        return StreamSupport.stream(data.spliterator(), false).map(JsonNode::asLong).toList();
    }
}
