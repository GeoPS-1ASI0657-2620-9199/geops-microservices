package com.geopslabs.geops.notification.application.ports;

import com.geopslabs.geops.notification.application.usecases.InboxNotificationView;
import java.time.Instant;
import java.util.List;

/** Reads the notices GeoPS decided to send (SENT or PENDING) for the bell of one recipient. */
public interface NotificationInboxRepositoryPort {
    List<InboxNotificationView> findLatest(Long recipientId, int size);
    long countUnread(Long recipientId);
    void markAllRead(Long recipientId, Instant readAt);
}
