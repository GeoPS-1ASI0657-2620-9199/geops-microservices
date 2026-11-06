package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.application.ports.NotificationChannelStrategy;
import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;

class PushFallbackTest {
    @Test
    void aFailedPushMovesTheSameNoticeIntoTheEmailDigest() {
        var notices = mock(NoticePlanningRepositoryPort.class); var unsub = mock(EmailUnsubscriptionRepositoryPort.class); var digests = mock(EmailDigestRepositoryPort.class); var push = mock(NotificationChannelStrategy.class);
        var clock = Clock.fixed(Instant.parse("2026-10-27T15:20:00Z"), ZoneOffset.UTC);
        when(notices.followers(84L)).thenReturn(List.of(2001L)); when(notices.lockPreferences(2001L)).thenReturn(Optional.of(new NotificationPreference(2001L, true, true, 3, LocalDateTime.now(clock))));
        when(notices.hasPushSubscription(2001L)).thenReturn(true); when(notices.recipient(2001L)).thenReturn(Optional.of(new Recipient(2001L, "consumer@example.test", true, "CONSUMER")));
        when(push.channel()).thenReturn(Channel.WEB_PUSH); when(push.available()).thenReturn(true); when(push.send(any(), any())).thenReturn(DeliveryStatus.FAILED);
        when(notices.save(any())).thenAnswer(i -> { com.geopslabs.geops.notification.domain.models.Notification n = i.getArgument(0); return new Notification(1L, n.getRecipientId(), n.getType(), n.getChannel(), n.getTitle(), n.getMessage(), n.getRelatedEntityId(), n.getStatus(), n.getSentAt()); });
        when(notices.findNotice(1L)).thenReturn(Optional.of(new Notification(1L, 2001L, NotificationType.FOLLOWED_BUSINESS_OFFER, Channel.WEB_PUSH, "2x1", "2x1", "1052", DeliveryStatus.PENDING, LocalDateTime.now(clock))));
        var planner = new NotificationPlanner(notices, unsub, digests, new EmailDigestService(digests, Duration.ofHours(1)), List.of(push), new TestTransactions(), clock);
        planner.offerPublished(new OfferPublishedEvent(1052L, 84L, "Don Pepe", "2x1", LocalDate.parse("2026-11-15"), new GeoPoint(-12.1, -77.0), null, clock.instant()));
        verify(notices).fallbackToEmail(1L); verify(notices, times(1)).save(any()); verify(digests).add(eq(2001L), eq(1052L), any(), any());
    }
}
