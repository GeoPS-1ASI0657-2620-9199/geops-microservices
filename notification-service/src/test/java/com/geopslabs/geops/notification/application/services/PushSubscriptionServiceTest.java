package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.models.exceptions.PushSubscriptionOwnershipException;
import com.geopslabs.geops.notification.domain.ports.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PushSubscriptionServiceTest {
    private final PushSubscriptionRepositoryPort subscriptions = mock(PushSubscriptionRepositoryPort.class);
    private final RecipientRepositoryPort recipients = mock(RecipientRepositoryPort.class);
    private final NotificationPreferenceRepositoryPort preferences = mock(NotificationPreferenceRepositoryPort.class);
    private final Clock clock = Clock.systemUTC();
    private final PushSubscriptionService service = new PushSubscriptionService(subscriptions, recipients, preferences, clock);
    @Test
    void theSameBrowserTokenKeepsItsSubscription() {
        var subscription = new PushSubscription(12L, 2001L, "token", "Browser", clock.instant());
        when(recipients.existsConsumer(2001L)).thenReturn(true);
        when(subscriptions.saveIfAbsent(any())).thenReturn(Optional.of(subscription), Optional.empty());
        when(subscriptions.findByToken("token")).thenReturn(Optional.of(subscription));
        assertThat(service.register(2001L, "token", "Browser").created()).isTrue();
        assertThat(service.register(2001L, "token", "Browser").created()).isFalse();
    }
    @Test
    void aTokenOfAnotherConsumerCannotBeTakenOverAndRemovalUsesTheOwner() {
        when(recipients.existsConsumer(2002L)).thenReturn(true);
        when(subscriptions.saveIfAbsent(any())).thenReturn(Optional.empty());
        when(subscriptions.findByToken("token")).thenReturn(Optional.of(new PushSubscription(12L, 2001L, "token", "Browser", clock.instant())));
        assertThatThrownBy(() -> service.register(2002L, "token", "Browser")).isInstanceOf(PushSubscriptionOwnershipException.class);
        service.remove(2002L, 12L); verify(subscriptions).delete(12L, 2002L);
    }
}
