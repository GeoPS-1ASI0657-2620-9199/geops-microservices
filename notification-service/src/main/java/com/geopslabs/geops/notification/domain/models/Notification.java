package com.geopslabs.geops.notification.domain.models;

import com.geopslabs.geops.notification.domain.models.commands.CreateNotificationCommand;
import com.geopslabs.geops.backend.identity.domain.model.aggregates.User;

import java.util.Date;

public class Notification {
    private Long id;
    private User user;
    private NotificationType type;
    private String title;
    private String message;
    private Boolean isRead;
    private String relatedEntityId;
    private String relatedEntityType;
    private String actionUrl;
    private Date createdAt;
    private Date updatedAt;

    public Notification(CreateNotificationCommand command, User user) {
        this.user = user;
        this.type = command.type();
        this.title = command.title();
        this.message = command.message();
        this.isRead = false;
        this.relatedEntityId = command.relatedEntityId();
        this.relatedEntityType = command.relatedEntityType();
        this.actionUrl = command.actionUrl();
    }

    @SuppressWarnings("java:S107")
    public Notification(Long id, User user, NotificationType type, String title, String message, Boolean isRead,
                        String relatedEntityId, String relatedEntityType, String actionUrl, Date createdAt,
                        Date updatedAt) {
        this.id = id;
        this.user = user;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isRead = isRead;
        this.relatedEntityId = relatedEntityId;
        this.relatedEntityType = relatedEntityType;
        this.actionUrl = actionUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public Long getUserId() {
        return this.user != null ? this.user.getId() : null;
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
