package com.geopslabs.geops.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.notification.application.usecases.ApplyFollowEventsUseCase;
import com.geopslabs.geops.notification.application.usecases.ApplyIdentityEventsUseCase;
import com.geopslabs.geops.notification.application.usecases.CancelOfferNoticesUseCase;
import com.geopslabs.geops.notification.application.usecases.NotifyReportPartiesUseCase;
import com.geopslabs.geops.notification.application.usecases.PlanOfferNoticesUseCase;
import com.geopslabs.geops.notification.domain.models.GeoPoint;
import com.geopslabs.geops.notification.domain.models.OfferPublishedEvent;
import com.geopslabs.geops.notification.domain.models.Recipient;
import com.geopslabs.geops.notification.domain.models.ReportNoticeEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Consumer side of the event contracts (US15, QAS10). Each example of platform/messaging/events that Notification
 * copied goes, as is, through the real listener and must reach the use case with the values of the example. The
 * publishers test what they write against the same files, so a field renamed on one side fails one of the builds
 * instead of sending the event to the dead letter topic in production.
 */
class ConsumedEventsContractTest {
    private final EventEnvelopeReader reader = new EventEnvelopeReader(new ObjectMapper());
    private final Acknowledgment acknowledgment = mock(Acknowledgment.class);

    @Test
    void offerPublishedPlansTheNoticesOfTheOffer() {
        var planner = mock(PlanOfferNoticesUseCase.class);

        new CatalogEventsListener(reader, planner, mock(CancelOfferNoticesUseCase.class))
                .onEvent(record("catalog.events", "OfferPublished"), acknowledgment);

        verify(planner).offerPublished(new OfferPublishedEvent(1052L, 84L, "Restaurante Don Pepe",
                "2x1 en almuerzos ejecutivos", LocalDate.parse("2026-11-15"), new GeoPoint(-12.1211, -77.0297),
                new OfferPublishedEvent.Zone(new GeoPoint(-12.1205, -77.0301), 1200),
                Instant.parse("2026-10-27T15:04:05Z")));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void offerExpiredAndCampaignPausedCancelThePendingNotices() {
        var cancel = mock(CancelOfferNoticesUseCase.class);
        var listener = new CatalogEventsListener(reader, mock(PlanOfferNoticesUseCase.class), cancel);

        listener.onEvent(record("catalog.events", "OfferExpired"), acknowledgment);
        listener.onEvent(record("catalog.events", "CampaignPaused"), acknowledgment);

        verify(cancel).offerUnavailable(List.of(1052L));
        verify(cancel).offerUnavailable(List.of(1052L, 1053L));
    }

    @Test
    void userRegisteredProjectsTheRecipient() {
        var identity = mock(ApplyIdentityEventsUseCase.class);

        new IdentityEventsListener(reader, identity).onEvent(record("identity.events", "UserRegistered"),
                acknowledgment);

        verify(identity).userRegistered(new Recipient(2001L, "lucia.fernandez@ejemplo.pe", false, "CONSUMER", null),
                Instant.parse("2026-10-27T14:00:00Z"));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumerPreferencesChangedUpdatesTheRadius() {
        var identity = mock(ApplyIdentityEventsUseCase.class);

        new IdentityEventsListener(reader, identity).onEvent(record("identity.events", "ConsumerPreferencesChanged"),
                acknowledgment);

        verify(identity).consumerPreferencesChanged(2001L, 10);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void followAndUnfollowUpdateTheFollowers() {
        var follows = mock(ApplyFollowEventsUseCase.class);
        var listener = new EngagementEventsListener(reader, follows);

        listener.onEvent(record("engagement.events", "BusinessFollowed"), acknowledgment);
        listener.onEvent(record("engagement.events", "BusinessUnfollowed"), acknowledgment);

        verify(follows).businessFollowed(2001L, 84L);
        verify(follows).businessUnfollowed(2001L, 84L);
    }

    @Test
    void theThreeReportEventsReachThePartiesOfTheReport() {
        var notices = mock(NotifyReportPartiesUseCase.class);
        var listener = new ReservationEventsListener(reader, notices);

        for (var eventType : List.of("ReportOpened", "ReportAnswered", "ReportResolved")) {
            listener.onEvent(record("reservation.events", eventType), acknowledgment);
        }

        verify(notices).apply(new ReportNoticeEvent("ReportOpened", 31L, 2001L, 84L, "Q4ZT8K2M",
                "2x1 en almuerzos ejecutivos", null));
        verify(notices).apply(new ReportNoticeEvent("ReportAnswered", 31L, 2001L, 84L, "Q4ZT8K2M",
                "2x1 en almuerzos ejecutivos", null));
        verify(notices).apply(new ReportNoticeEvent("ReportResolved", 31L, 2001L, 84L, "Q4ZT8K2M",
                "2x1 en almuerzos ejecutivos", "CONSUMER_FAVOR"));
    }

    private static ConsumerRecord<String, String> record(String topic, String eventType) {
        return new ConsumerRecord<>(topic, 0, 0L, "1", EventFixtures.read(eventType));
    }
}
