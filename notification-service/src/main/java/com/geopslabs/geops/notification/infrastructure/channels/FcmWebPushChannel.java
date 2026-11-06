package com.geopslabs.geops.notification.infrastructure.channels;

import com.geopslabs.geops.notification.application.ports.NotificationChannelStrategy;
import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.PushSubscriptionRepositoryPort;
import com.google.firebase.messaging.*;
import org.slf4j.*;

public class FcmWebPushChannel implements NotificationChannelStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(FcmWebPushChannel.class);
    private final FirebaseMessaging messaging;
    private final PushSubscriptionRepositoryPort subscriptions;
    private final String webBaseUrl;
    public FcmWebPushChannel(FirebaseMessaging messaging, PushSubscriptionRepositoryPort subscriptions, String webBaseUrl) {
        this.messaging = messaging; this.subscriptions = subscriptions; this.webBaseUrl = webBaseUrl;
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
        try { messaging.send(message); return true; }
        catch (FirebaseMessagingException error) { removeInvalidToken(error, subscription); return false; }
        catch (RuntimeException error) { LOGGER.warn("notification.push.failed recipientId={} reason={}", subscription.consumerId(), error.getClass().getSimpleName()); return false; }
    }
    private void removeInvalidToken(FirebaseMessagingException error, PushSubscription subscription) {
        if (error.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED || error.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) subscriptions.delete(subscription.id(), subscription.consumerId());
        LOGGER.warn("notification.push.failed recipientId={} reason={}", subscription.consumerId(), error.getMessagingErrorCode());
    }
}
