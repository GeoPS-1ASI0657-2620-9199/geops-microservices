package com.geopslabs.geops.notification.acceptance;

import com.geopslabs.geops.notification.application.ports.NotificationChannelStrategy;
import com.geopslabs.geops.notification.domain.models.*;
import java.util.*;
public class RecordingPushChannel implements NotificationChannelStrategy {
    private final List<Notification> messages = new ArrayList<>();
    @Override public Channel channel() { return Channel.WEB_PUSH; }
    @Override public DeliveryStatus send(Notification notice, Recipient recipient) { messages.add(notice); return DeliveryStatus.SENT; }
    public void clear() { messages.clear(); }
    public List<Notification> messages() { return List.copyOf(messages); }
}
