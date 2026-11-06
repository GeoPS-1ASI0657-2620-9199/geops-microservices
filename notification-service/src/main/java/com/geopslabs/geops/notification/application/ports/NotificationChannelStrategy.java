package com.geopslabs.geops.notification.application.ports;

import com.geopslabs.geops.notification.domain.models.*;
public interface NotificationChannelStrategy {
    Channel channel();
    DeliveryStatus send(Notification notification, Recipient recipient);
    default boolean available() { return true; }
}
