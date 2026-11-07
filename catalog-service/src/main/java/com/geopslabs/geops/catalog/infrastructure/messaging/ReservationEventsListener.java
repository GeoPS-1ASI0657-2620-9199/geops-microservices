package com.geopslabs.geops.catalog.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.geopslabs.geops.catalog.application.usecases.ApplyComplianceEventUseCase;
import com.geopslabs.geops.catalog.application.usecases.ComplianceEvent;
import com.geopslabs.geops.catalog.domain.models.ComplianceChange;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Consumer of reservation.events for the ranking (US44, Table 48). ReservationRedeemed and the three report events
 * change the compliance of the business; every other event type is acknowledged and ignored. The event is
 * acknowledged only after the standing is committed and its cells are deleted. What cannot be read goes to the
 * dead letter topic through the error handler of MessagingConfiguration.
 */
@Component
public class ReservationEventsListener {
    static final String RESERVATION_REDEEMED = "ReservationRedeemed";
    static final String REPORT_OPENED = "ReportOpened";
    static final String REPORT_ANSWERED = "ReportAnswered";
    static final String REPORT_RESOLVED = "ReportResolved";
    private static final int SUPPORTED_VERSION = 1;
    private static final String BUSINESS_ID = "businessId";
    private static final String PREVIOUS_STATUS = "previousStatus";
    private static final String RESOLUTION = "resolution";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationEventsListener.class);

    private final ApplyComplianceEventUseCase complianceEvents;
    private final EventEnvelopeReader envelopes;

    public ReservationEventsListener(ApplyComplianceEventUseCase complianceEvents, EventEnvelopeReader envelopes) {
        this.complianceEvents = complianceEvents;
        this.envelopes = envelopes;
    }

    @KafkaListener(id = "catalog-reservation-events", topics = "${geops.events.reservation-topic:reservation.events}",
            groupId = "catalog-service", autoStartup = "${geops.messaging.enabled:true}")
    public void onReservationEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        var envelope = envelopes.read(record.value());
        var change = changeOf(envelope);
        if (change.isEmpty()) {
            LOGGER.debug("reservation.event.ignored eventType={} eventId={}", envelope.eventType(),
                    envelope.eventId());
            acknowledgment.acknowledge();
            return;
        }
        envelopes.requireVersion(envelope, SUPPORTED_VERSION);
        complianceEvents.apply(new ComplianceEvent(envelope.eventId(), envelope.eventType(), envelope.occurredAt(),
                businessIdOf(envelope), change.get()));
        acknowledgment.acknowledge();
    }

    static Optional<ComplianceChange> changeOf(EventEnvelope envelope) {
        return Optional.ofNullable(switch (envelope.eventType()) {
            case RESERVATION_REDEEMED -> ComplianceChange.forRedemption();
            case REPORT_OPENED -> ComplianceChange.forReportOpened();
            case REPORT_ANSWERED -> ComplianceChange.forReportAnswered();
            case REPORT_RESOLVED -> ComplianceChange.forReportResolved(
                    requiredText(envelope, PREVIOUS_STATUS), requiredText(envelope, RESOLUTION));
            default -> null;
        });
    }

    private static Long businessIdOf(EventEnvelope envelope) {
        var businessId = envelope.payload().path(BUSINESS_ID);
        if (!businessId.canConvertToLong()) {
            throw new UnreadableEventException(envelope.eventType() + " without businessId");
        }
        return businessId.asLong();
    }

    private static String requiredText(EventEnvelope envelope, String field) {
        JsonNode value = envelope.payload().path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new UnreadableEventException(envelope.eventType() + " without " + field);
        }
        return value.asText();
    }
}
