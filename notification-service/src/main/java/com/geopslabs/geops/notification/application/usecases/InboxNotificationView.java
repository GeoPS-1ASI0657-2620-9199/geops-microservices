package com.geopslabs.geops.notification.application.usecases;

import java.time.Instant;

public record InboxNotificationView(Long id, String type, String title, String message, Long offerId, Instant createdAt, boolean read) { }
