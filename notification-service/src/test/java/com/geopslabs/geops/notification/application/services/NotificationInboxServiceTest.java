package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.application.ports.NotificationInboxRepositoryPort;
import com.geopslabs.geops.notification.application.usecases.InboxNotificationView;
import com.geopslabs.geops.notification.domain.models.exceptions.InvalidInboxSizeException;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationInboxServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-23T15:00:00Z");
    private final NotificationInboxRepositoryPort inbox = mock(NotificationInboxRepositoryPort.class);
    private final NotificationInboxService service = new NotificationInboxService(inbox, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void listsTheLatestNoticesOfTheCallerOnly() {
        var view = new InboxNotificationView(14L, "NEARBY_OFFER", "Oferta cerca de ti", "Mensaje", 30073L, NOW, false);
        when(inbox.findLatest(2001L, 20)).thenReturn(List.of(view));
        assertThat(service.list(2001L, 20)).containsExactly(view);
        verify(inbox).findLatest(2001L, 20);
        verifyNoMoreInteractions(inbox);
    }

    @Test
    void acceptsTheLimitsOfTheSizeAndRejectsWhatIsOutside() {
        service.list(1L, 1);
        service.list(1L, 50);
        assertThatThrownBy(() -> service.list(1L, 0)).isInstanceOf(InvalidInboxSizeException.class);
        assertThatThrownBy(() -> service.list(1L, 51)).isInstanceOf(InvalidInboxSizeException.class);
        verify(inbox, never()).findLatest(1L, 51);
    }

    @Test
    void countsTheUnreadNoticesOfTheCaller() {
        when(inbox.countUnread(2001L)).thenReturn(3L);
        assertThat(service.count(2001L)).isEqualTo(3L);
    }

    @Test
    void marksAsReadAtTheCurrentInstantForTheCallerOnly() {
        service.markAllRead(2001L);
        verify(inbox).markAllRead(2001L, NOW);
        verifyNoMoreInteractions(inbox);
    }
}
