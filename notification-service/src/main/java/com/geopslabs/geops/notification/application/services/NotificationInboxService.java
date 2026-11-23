package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.application.ports.NotificationInboxRepositoryPort;
import com.geopslabs.geops.notification.application.usecases.*;
import com.geopslabs.geops.notification.domain.models.exceptions.InvalidInboxSizeException;
import java.time.Clock;
import java.util.List;

public class NotificationInboxService implements ListMyNotificationsUseCase, CountUnreadNotificationsUseCase, MarkNotificationsReadUseCase {
    static final int MIN_SIZE = 1;
    static final int MAX_SIZE = 50;
    private final NotificationInboxRepositoryPort inbox;
    private final Clock clock;
    public NotificationInboxService(NotificationInboxRepositoryPort inbox, Clock clock) { this.inbox = inbox; this.clock = clock; }
    @Override
    public List<InboxNotificationView> list(Long recipientId, int size) {
        if (size < MIN_SIZE || size > MAX_SIZE) throw new InvalidInboxSizeException();
        return inbox.findLatest(recipientId, size);
    }
    @Override
    public long count(Long recipientId) { return inbox.countUnread(recipientId); }
    @Override
    public void markAllRead(Long recipientId) { inbox.markAllRead(recipientId, clock.instant()); }
}
