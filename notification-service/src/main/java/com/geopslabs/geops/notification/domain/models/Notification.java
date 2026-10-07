package com.geopslabs.geops.notification.domain.models;

import java.time.LocalDateTime;

public class Notification {
    private final Long id;
    private final Long recipientId;
    private final NotificationType type;
    private final Channel channel;
    private final String title;
    private final String message;
    private final String relatedEntityId;
    private final DeliveryStatus status;
    private final LocalDateTime sentAt;

    @SuppressWarnings("java:S107")
    public Notification(Long id, Long recipientId, NotificationType type, Channel channel, String title,
                        String message, String relatedEntityId, DeliveryStatus status, LocalDateTime sentAt) {
        this.id = id;
        this.recipientId = recipientId;
        this.type = type;
        this.channel = channel;
        this.title = title;
        this.message = message;
        this.relatedEntityId = relatedEntityId;
        this.status = status;
        this.sentAt = sentAt;
    }

    public static Notification draft(Long recipientId, NotificationType type, Channel channel, String title,
                                     String message, String relatedEntityId) {
        return new Notification(null, recipientId, type, channel, title, message, relatedEntityId, null, null);
    }

    public Notification delivered(DeliveryStatus deliveryStatus, LocalDateTime deliveredAt) {
        return new Notification(id, recipientId, type, channel, title, message, relatedEntityId, deliveryStatus,
                deliveredAt);
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

    public Channel getChannel() {
        return channel;
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

    public DeliveryStatus getStatus() {
        return status;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
