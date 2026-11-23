package com.geopslabs.geops.notification.infrastructure.channels;

import com.geopslabs.geops.notification.application.ports.NotificationChannelStrategy;
import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.PushSubscriptionRepositoryPort;
import com.google.firebase.messaging.*;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.*;

/**
 * Web push through FCM (US29). Each message goes through the circuit breaker and the bulkhead "fcm" (US34): with the
 * circuit open or the bulkhead full FCM is not called and the push counts as failed, so the planner moves the notice
 * to the e-mail digest. A token that FCM no longer accepts is an answer, not an outage: it is deleted and does not
 * count toward opening the circuit.
 */
public class FcmWebPushChannel implements NotificationChannelStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(FcmWebPushChannel.class);
    private final FirebaseMessaging messaging;
    private final PushSubscriptionRepositoryPort subscriptions;
    private final String webBaseUrl;
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;
    public FcmWebPushChannel(FirebaseMessaging messaging, PushSubscriptionRepositoryPort subscriptions, String webBaseUrl,
                             CircuitBreaker circuitBreaker, Bulkhead bulkhead) {
        this.messaging = messaging; this.subscriptions = subscriptions; this.webBaseUrl = webBaseUrl;
        this.circuitBreaker = circuitBreaker; this.bulkhead = bulkhead;
        if (messaging == null) LOGGER.info("notification.push.disabled");
    }
    @Override public Channel channel() { return Channel.WEB_PUSH; }
    @Override public boolean available() { return messaging != null; }
    @Override
    public DeliveryStatus send(com.geopslabs.geops.notification.domain.models.Notification notification, Recipient recipient) {
        if (!available()) return DeliveryStatus.FAILED;
        var sent = subscriptions.findByConsumer(recipient.userId()).stream().map(s -> send(notification, s)).toList();
        return sent.contains(Boolean.TRUE) ? DeliveryStatus.SENT : DeliveryStatus.FAILED;
    }
    private boolean send(com.geopslabs.geops.notification.domain.models.Notification notice, PushSubscription subscription) {
        var link = notice.getRelatedEntityId().startsWith("report-") ? "/reservations" : "/offers/" + notice.getRelatedEntityId();
        var message = Message.builder().setToken(subscription.fcmToken()).setWebpushConfig(WebpushConfig.builder()
                .setNotification(WebpushNotification.builder().setTitle(notice.getTitle()).setBody(notice.getMessage()).setIcon(webBaseUrl + "/favicon.ico").build())
                .setFcmOptions(WebpushFcmOptions.withLink(webBaseUrl + link)).build()).build();
        try { return circuitBreaker.executeCallable(() -> bulkhead.executeCallable(() -> deliver(message, subscription))); }
        catch (CallNotPermittedException error) { LOGGER.warn("notification.push.failed recipientId={} reason=circuit-open", subscription.consumerId()); return false; }
        catch (BulkheadFullException error) { LOGGER.warn("notification.push.failed recipientId={} reason=bulkhead-full", subscription.consumerId()); return false; }
        catch (FirebaseMessagingException error) { LOGGER.warn("notification.push.failed recipientId={} reason={}", subscription.consumerId(), error.getMessagingErrorCode()); return false; }
        catch (Exception error) { LOGGER.warn("notification.push.failed recipientId={} reason={}", subscription.consumerId(), error.getClass().getSimpleName()); return false; }
    }
    /** An invalid token returns false instead of throwing, so the circuit breaker records it as a call that worked. */
    private boolean deliver(Message message, PushSubscription subscription) throws FirebaseMessagingException {
        try { messaging.send(message); return true; }
        catch (FirebaseMessagingException error) {
            if (!isInvalidToken(error)) throw error;
            subscriptions.delete(subscription.id(), subscription.consumerId());
            LOGGER.warn("notification.push.failed recipientId={} reason={}", subscription.consumerId(), error.getMessagingErrorCode());
            return false;
        }
    }
    private static boolean isInvalidToken(FirebaseMessagingException error) {
        return error.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED || error.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT;
    }
}
