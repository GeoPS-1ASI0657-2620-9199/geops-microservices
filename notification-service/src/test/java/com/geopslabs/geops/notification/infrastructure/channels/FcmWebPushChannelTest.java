package com.geopslabs.geops.notification.infrastructure.channels;

import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.PushSubscriptionRepositoryPort;
import com.google.firebase.messaging.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FcmWebPushChannelTest {
    private final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
    private final PushSubscriptionRepositoryPort subscriptions = mock(PushSubscriptionRepositoryPort.class);
    private final Recipient recipient = new Recipient(2001L, "consumer@example.test", true, "CONSUMER");
    private final com.geopslabs.geops.notification.domain.models.Notification notice = new com.geopslabs.geops.notification.domain.models.Notification(1L, 2001L, NotificationType.NEARBY_OFFER, Channel.WEB_PUSH, "2x1", "A 300 m", "1052", DeliveryStatus.PENDING, LocalDateTime.now());
    @Test
    void sendsThroughTheSdkAndDeletesUnregisteredTokens() throws Exception {
        when(subscriptions.findByConsumer(2001L)).thenReturn(List.of(new PushSubscription(12L, 2001L, "token", "Browser", Instant.now())));
        var channel = new FcmWebPushChannel(messaging, subscriptions, "https://geops.example");
        when(messaging.send(any(Message.class))).thenReturn("sent");
        assertThat(channel.send(notice, recipient)).isEqualTo(DeliveryStatus.SENT);
        var error = mock(FirebaseMessagingException.class); when(error.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(messaging.send(any(Message.class))).thenThrow(error);
        assertThat(channel.send(notice, recipient)).isEqualTo(DeliveryStatus.FAILED);
        verify(subscriptions).delete(12L, 2001L);
    }
    @Test
    void withoutCredentialsPushIsUnavailable() {
        var channel = new FcmWebPushChannel(null, subscriptions, "https://geops.example");
        assertThat(channel.available()).isFalse(); assertThat(channel.send(notice, recipient)).isEqualTo(DeliveryStatus.FAILED);
        verifyNoInteractions(subscriptions);
    }
}
