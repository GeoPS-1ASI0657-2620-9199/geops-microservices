package com.geopslabs.geops.notification.domain.models;

import com.geopslabs.geops.notification.domain.models.commands.CreateNotificationCommand;

import java.time.LocalDateTime;

public class Notification {
    private Long id;
    private Long recipientId;
    private NotificationType type;
    private String title;
    private String message;
    private Boolean isRead;
    private String relatedEntityId;
    private String relatedEntityType;
    private String actionUrl;
    private LocalDateTime createdAt;

    public Notification(CreateNotificationCommand command) {
        this.recipientId = command.recipientId();
        this.type = command.type();
        this.title = command.title();
        this.message = command.message();
        this.isRead = false;
        this.relatedEntityId = command.relatedEntityId();
        this.relatedEntityType = command.relatedEntityType();
        this.actionUrl = command.actionUrl();
    }

    @SuppressWarnings("java:S107")
    public Notification(Long id, Long recipientId, NotificationType type, String title, String message,
                        Boolean isRead, String relatedEntityId, String relatedEntityType, String actionUrl,
                        LocalDateTime createdAt) {
        this.id = id;
        this.recipientId = recipientId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isRead = isRead;
        this.relatedEntityId = relatedEntityId;
        this.relatedEntityType = relatedEntityType;
        this.actionUrl = actionUrl;
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

    public Boolean getIsRead() {
        return isRead;
    }

    public String getRelatedEntityId() {
        return relatedEntityId;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public void markAsUnread() {
        this.isRead = false;
    }

    public boolean isUnread() {
        return !this.isRead;
    }
}
