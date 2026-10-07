package com.geopslabs.geops.notification.domain.models;

import java.time.LocalDateTime;

public class Notification {
    private final Long id;
    private final Long recipientId;
    private final NotificationType type;
    private final String title;
    private final String message;
    private final String relatedEntityId;
    private final LocalDateTime createdAt;

    public Notification(Long id, Long recipientId, NotificationType type, String title, String message,
                        String relatedEntityId, LocalDateTime createdAt) {
        this.id = id;
        this.recipientId = recipientId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedEntityId = relatedEntityId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getRelatedEntityId() {
        return relatedEntityId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
