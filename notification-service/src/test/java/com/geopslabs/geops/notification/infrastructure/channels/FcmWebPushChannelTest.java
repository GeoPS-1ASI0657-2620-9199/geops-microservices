package com.geopslabs.geops.notification.infrastructure.channels;

import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.PushSubscriptionRepositoryPort;
import com.google.firebase.messaging.*;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static com.geopslabs.geops.notification.infrastructure.channels.ProviderResilience.FAILURES_THAT_OPEN;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FcmWebPushChannelTest {
    private final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
    private final PushSubscriptionRepositoryPort subscriptions = mock(PushSubscriptionRepositoryPort.class);
    private final CircuitBreaker circuitBreaker = ProviderResilience.circuitBreaker("fcm");
    private final Bulkhead bulkhead = ProviderResilience.singlePlaceBulkhead("fcm");
    private final Recipient recipient = new Recipient(2001L, "consumer@example.test", true, "CONSUMER");
    private final com.geopslabs.geops.notification.domain.models.Notification notice = new com.geopslabs.geops.notification.domain.models.Notification(1L, 2001L, NotificationType.NEARBY_OFFER, Channel.WEB_PUSH, "2x1", "A 300 m", "1052", DeliveryStatus.PENDING, LocalDateTime.now());
    @Test
    void sendsThroughTheSdkAndDeletesUnregisteredTokens() throws Exception {
        subscribed();
        var channel = channel();
        when(messaging.send(any(Message.class))).thenReturn("sent");
        assertThat(channel.send(notice, recipient)).isEqualTo(DeliveryStatus.SENT);
        var error = firebaseError(MessagingErrorCode.UNREGISTERED);
        when(messaging.send(any(Message.class))).thenThrow(error);
        assertThat(channel.send(notice, recipient)).isEqualTo(DeliveryStatus.FAILED);
        verify(subscriptions).delete(12L, 2001L);
    }
    @Test
    void withoutCredentialsPushIsUnavailable() {
        var channel = new FcmWebPushChannel(null, subscriptions, "https://geops.example", circuitBreaker, bulkhead);
        assertThat(channel.available()).isFalse(); assertThat(channel.send(notice, recipient)).isEqualTo(DeliveryStatus.FAILED);
        verifyNoInteractions(subscriptions);
    }
    @Test
    void fiveFcmOutagesOpenTheCircuitAndTheNextPushFailsWithoutCallingFcm() throws Exception {
        subscribed();
        var channel = channel();
        var outage = firebaseError(MessagingErrorCode.UNAVAILABLE);
        when(messaging.send(any(Message.class))).thenThrow(outage);
        for (int attempt = 0; attempt < FAILURES_THAT_OPEN; attempt++) assertThat(channel.send(notice, recipient)).isEqualTo(DeliveryStatus.FAILED);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(channel.send(notice, recipient)).isEqualTo(DeliveryStatus.FAILED);
        verify(messaging, times(FAILURES_THAT_OPEN)).send(any(Message.class));
        verify(subscriptions, never()).delete(anyLong(), anyLong());
    }
    @Test
    void timeoutsOfTheSdkCountAsFailures() throws Exception {
        subscribed();
        var channel = channel();
        when(messaging.send(any(Message.class))).thenThrow(new IllegalStateException("read timed out"));
        for (int attempt = 0; attempt < FAILURES_THAT_OPEN; attempt++) channel.send(notice, recipient);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
    @Test
    void tokensThatFcmRejectsAreAnAnswerAndDoNotOpenTheCircuit() throws Exception {
        subscribed();
        var channel = channel();
        var error = firebaseError(MessagingErrorCode.INVALID_ARGUMENT);
        when(messaging.send(any(Message.class))).thenThrow(error);
        for (int attempt = 0; attempt < FAILURES_THAT_OPEN + 1; attempt++) channel.send(notice, recipient);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }
    @Test
    void aSuccessfulProbeClosesTheCircuitAgain() throws Exception {
        subscribed();
        when(messaging.send(any(Message.class))).thenReturn("sent");
        circuitBreaker.transitionToOpenState(); circuitBreaker.transitionToHalfOpenState();

        assertThat(channel().send(notice, recipient)).isEqualTo(DeliveryStatus.SENT);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
    @Test
    void aFullBulkheadFailsThePushAtOnceWithoutCallingFcmOrOpeningTheCircuit() throws Exception {
        subscribed();
        var channel = channel();
        assertThat(bulkhead.tryAcquirePermission()).isTrue();

        assertThat(channel.send(notice, recipient)).isEqualTo(DeliveryStatus.FAILED);
        verify(messaging, never()).send(any(Message.class));
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }
    private FcmWebPushChannel channel() { return new FcmWebPushChannel(messaging, subscriptions, "https://geops.example", circuitBreaker, bulkhead); }
    private void subscribed() { when(subscriptions.findByConsumer(2001L)).thenReturn(List.of(new PushSubscription(12L, 2001L, "token", "Browser", Instant.now()))); }
    private static FirebaseMessagingException firebaseError(MessagingErrorCode code) {
        var error = mock(FirebaseMessagingException.class); when(error.getMessagingErrorCode()).thenReturn(code); return error;
    }
}
