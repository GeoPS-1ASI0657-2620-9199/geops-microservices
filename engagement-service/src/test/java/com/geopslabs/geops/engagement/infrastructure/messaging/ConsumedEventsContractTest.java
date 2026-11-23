package com.geopslabs.geops.engagement.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.engagement.application.usecases.ProjectCatalogEventsUseCase;
import com.geopslabs.geops.engagement.application.usecases.ProjectRedemptionUseCase;
import com.geopslabs.geops.engagement.domain.models.BusinessSnapshot;
import com.geopslabs.geops.engagement.domain.models.OfferSnapshot;
import com.geopslabs.geops.engagement.domain.models.RedeemedReservation;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Consumer side of the event contracts (US15, QAS10). Each example of platform/messaging/events that Engagement
 * copied is passed, as is, through the real listener: it must reach the use case with the values of the example
 * and be acknowledged. Catalog and Reservation test what they publish against the same files.
 */
class ConsumedEventsContractTest {
    private final EventEnvelopeReader reader = new EventEnvelopeReader(new ObjectMapper());
    private final Acknowledgment acknowledgment = mock(Acknowledgment.class);
    private final ProjectCatalogEventsUseCase catalog = mock(ProjectCatalogEventsUseCase.class);
    private final ProjectRedemptionUseCase redemptions = mock(ProjectRedemptionUseCase.class);

    @Test
    void offerPublishedProjectsTheBusinessAndTheOffer() {
        new CatalogEventsListener(reader, catalog).onEvent(record("catalog.events", "OfferPublished"), acknowledgment);

        verify(catalog).offerPublished(new BusinessSnapshot(84L, "Restaurante Don Pepe"),
                new OfferSnapshot(1052L, 84L, "2x1 en almuerzos ejecutivos", LocalDate.parse("2026-11-15"),
                        OfferSnapshot.PUBLISHED), Instant.parse("2026-10-27T15:04:05Z"));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void campaignPausedWithdrawsEveryOfferOfTheCampaign() {
        new CatalogEventsListener(reader, catalog).onEvent(record("catalog.events", "CampaignPaused"), acknowledgment);

        verify(catalog).campaignPaused(List.of(1052L, 1053L), Instant.parse("2026-10-28T10:00:00Z"));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void offerExpiredWithdrawsTheOffer() {
        new CatalogEventsListener(reader, catalog).onEvent(record("catalog.events", "OfferExpired"), acknowledgment);

        verify(catalog).offerExpired(1052L, Instant.parse("2026-11-16T05:00:00Z"));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void reservationRedeemedOpensTheReviewOfTheConsumer() {
        new ReservationEventsListener(reader, redemptions)
                .onEvent(record("reservation.events", "ReservationRedeemed"), acknowledgment);

        verify(redemptions).reservationRedeemed(new RedeemedReservation(5012L, 2001L, 84L,
                LocalDateTime.parse("2026-10-20T17:42:10")));
        verify(acknowledgment).acknowledge();
    }

    private static ConsumerRecord<String, String> record(String topic, String eventType) {
        return new ConsumerRecord<>(topic, 0, 0L, "1", EventFixtures.read(eventType));
    }
}
