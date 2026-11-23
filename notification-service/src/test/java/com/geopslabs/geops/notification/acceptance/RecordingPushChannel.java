package com.geopslabs.geops.notification.acceptance;

import com.geopslabs.geops.notification.application.ports.NotificationChannelStrategy;
import com.geopslabs.geops.notification.domain.models.*;
import java.util.*;
public class RecordingPushChannel implements NotificationChannelStrategy {
    private final List<Notification> messages = new ArrayList<>();
    private volatile NotificationChannelStrategy realChannel;
    @Override public Channel channel() { return Channel.WEB_PUSH; }
    @Override public DeliveryStatus send(Notification notice, Recipient recipient) {
        var result = realChannel == null ? DeliveryStatus.SENT : realChannel.send(notice, recipient);
        if (result == DeliveryStatus.SENT) messages.add(notice);
        return result;
    }
    /** US34: from here on the push goes through the real FCM channel, with its circuit breaker. */
    public void useRealChannel(NotificationChannelStrategy channel) { realChannel = channel; }
    public void clear() { messages.clear(); realChannel = null; }
    public List<Notification> messages() { return List.copyOf(messages); }
}
