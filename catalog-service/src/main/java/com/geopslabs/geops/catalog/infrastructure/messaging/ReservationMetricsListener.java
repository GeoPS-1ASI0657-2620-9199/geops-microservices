package com.geopslabs.geops.catalog.infrastructure.messaging;

import com.geopslabs.geops.catalog.application.usecases.ApplyReservationMetricEventUseCase;
import com.geopslabs.geops.catalog.application.usecases.ReservationMetricEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Consumer of reservation.events for the campaign metrics (US11): ReservationCreated adds a reservation and
 * ReservationRedeemed a redemption to the campaign of the offer; every other event type is acknowledged and ignored.
 * It has its own consumer group, so its offset, retries and dead letters do not depend on the reputation consumer
 * that reads the same topic; a new group starts from the beginning of the topic and counts the redemptions
 * published before it existed. What cannot be read goes to the dead letter topic through MessagingConfiguration.
 */
@Component
public class ReservationMetricsListener {
    public static final String GROUP = "catalog-campaign-metrics";
    static final String RESERVATION_CREATED = "ReservationCreated";
    static final String RESERVATION_REDEEMED = "ReservationRedeemed";
    private static final int SUPPORTED_VERSION = 1;
    private static final String OFFER_ID = "offerId";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationMetricsListener.class);

    private final ApplyReservationMetricEventUseCase metrics;
    private final EventEnvelopeReader envelopes;

    public ReservationMetricsListener(ApplyReservationMetricEventUseCase metrics, EventEnvelopeReader envelopes) {
        this.metrics = metrics;
        this.envelopes = envelopes;
    }

    @KafkaListener(id = GROUP, topics = "${geops.events.reservation-topic:reservation.events}", groupId = GROUP,
            autoStartup = "${geops.messaging.enabled:true}")
    public void onReservationEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        var envelope = envelopes.read(record.value());
        var kind = kindOf(envelope.eventType());
        if (kind.isEmpty()) {
            LOGGER.debug("campaign.metrics.event.ignored eventType={} eventId={}", envelope.eventType(),
                    envelope.eventId());
            acknowledgment.acknowledge();
            return;
        }
        envelopes.requireVersion(envelope, SUPPORTED_VERSION);
        metrics.apply(new ReservationMetricEvent(envelope.eventId(), envelope.eventType(), kind.get(),
                offerIdOf(envelope), envelope.occurredAt()));
        acknowledgment.acknowledge();
    }

    static Optional<ReservationMetricEvent.Kind> kindOf(String eventType) {
        return Optional.ofNullable(switch (eventType) {
            case RESERVATION_CREATED -> ReservationMetricEvent.Kind.RESERVED;
            case RESERVATION_REDEEMED -> ReservationMetricEvent.Kind.REDEEMED;
            default -> null;
        });
    }

    private static Long offerIdOf(EventEnvelope envelope) {
        var offerId = envelope.payload().path(OFFER_ID);
        if (!offerId.canConvertToLong()) {
            throw new UnreadableEventException(envelope.eventType() + " without offerId");
        }
        return offerId.asLong();
    }
}
