package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationPlannerTest {
    private final NoticePlanningRepositoryPort repository = mock(NoticePlanningRepositoryPort.class);
    private final EmailUnsubscriptionRepositoryPort unsubscriptions = mock(EmailUnsubscriptionRepositoryPort.class);
    private final EmailDigestRepositoryPort digests = mock(EmailDigestRepositoryPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-27T15:20:00Z"), ZoneOffset.UTC);
    private final NotificationPlanner planner = new NotificationPlanner(repository, unsubscriptions, digests, new EmailDigestService(digests, Duration.ofHours(1)), List.of(), new TestTransactions(), clock);
    private final OfferPublishedEvent offer = new OfferPublishedEvent(1052L, 84L, "Don Pepe", "2x1", LocalDate.parse("2026-11-15"), new GeoPoint(-12.1211, -77.0297), null, clock.instant());
    @BeforeEach
    void consumer() {
        when(repository.nearby(any(), any())).thenReturn(List.of(new NearbyConsumer(2001L, 300)));
        when(repository.lockPreferences(2001L)).thenReturn(Optional.of(new NotificationPreference(2001L, false, true, 3, LocalDateTime.now(clock))));
        when(repository.recipient(2001L)).thenReturn(Optional.of(new Recipient(2001L, "consumer@example.test", true, "CONSUMER")));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    @Test
    void followerTakesPrecedenceOverProximityAndEmailIsGrouped() {
        when(repository.followers(84L)).thenReturn(List.of(2001L));
        planner.offerPublished(offer);
        verify(repository).save(argThat(n -> n.getType() == NotificationType.FOLLOWED_BUSINESS_OFFER && n.getStatus() == DeliveryStatus.PENDING));
        verify(digests).add(2001L, 1052L, Instant.parse("2026-10-27T15:00:00Z"), Instant.parse("2026-10-27T16:00:00Z"));
    }
    @Test
    void reachingTheQuotaSkipsAcrossChannels() {
        when(repository.countOfferNoticesSince(eq(2001L), any(), any())).thenReturn(3L);
        planner.offerPublished(offer);
        verify(repository).save(argThat(n -> n.getStatus() == DeliveryStatus.SKIPPED_BY_LIMIT));
        verifyNoInteractions(digests);
    }
    @Test
    void aRepeatedOfferDoesNotUseAnotherDailySlot() {
        when(repository.existsOfferNotice(2001L, 1052L)).thenReturn(true);
        planner.offerPublished(offer);
        verify(repository, never()).save(any());
    }
    @Test
    void staleOffersAndStaleLocationsDoNotNotify() {
        var old = new OfferPublishedEvent(1052L, 84L, "Don Pepe", "2x1", offer.validTo(), offer.location(), null, clock.instant().minus(Duration.ofMinutes(61)));
        planner.offerPublished(old);
        verify(repository, never()).lockPreferences(any());
        when(repository.nearby(any(), any())).thenReturn(List.of());
        planner.offerPublished(offer);
        verify(repository, never()).save(any());
    }
    @Test
    void unconfirmedEmailUnsubscriptionOrDisabledChannelsPreventDelivery() {
        when(unsubscriptions.exists(2001L, NotificationType.NEARBY_OFFER)).thenReturn(true);
        planner.offerPublished(offer);
        verify(repository, never()).save(any());
        when(unsubscriptions.exists(any(), any())).thenReturn(false);
        when(repository.recipient(2001L)).thenReturn(Optional.of(new Recipient(2001L, "consumer@example.test", false, "CONSUMER")));
        planner.offerPublished(offer);
        verify(repository, never()).save(any());
        when(repository.lockPreferences(2001L)).thenReturn(Optional.of(new NotificationPreference(2001L, false, false, 3, LocalDateTime.now(clock))));
        planner.offerPublished(offer);
        verify(repository, never()).save(any());
    }
}
