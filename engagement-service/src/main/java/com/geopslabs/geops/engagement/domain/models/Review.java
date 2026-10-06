package com.geopslabs.geops.engagement.domain.models;

import com.geopslabs.geops.engagement.domain.models.commands.CreateReviewCommand;
import com.geopslabs.geops.engagement.domain.models.commands.UpdateReviewCommand;

import java.time.LocalDateTime;

public class Review {
    private Long id;
    private Long reservationId;
    private Long consumerId;
    private Long businessId;
    private String userName;
    private Integer rating;
    private String text;
    private Integer likes;
    private LocalDateTime createdAt;

    public Review(CreateReviewCommand command) {
        this.reservationId = command.reservationId();
        this.consumerId = command.consumerId();
        this.businessId = command.businessId();
        this.userName = command.userName();
        this.rating = command.rating();
        this.text = command.text();
        this.likes = 0;
    }

    public Review(Long id, Long reservationId, Long consumerId, Long businessId, String userName, Integer rating,
                  String text, Integer likes, LocalDateTime createdAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.consumerId = consumerId;
        this.businessId = businessId;
        this.userName = userName;
        this.rating = rating;
        this.text = text;
        this.likes = likes;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public Long getConsumerId() {
        return consumerId;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public String getUserName() {
        return userName;
    }

    public Integer getRating() {
        return rating;
    }

    public String getText() {
        return text;
    }

    public Integer getLikes() {
        return likes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void updateReview(UpdateReviewCommand command) {
        if (command.text() != null) {
            this.text = command.text();
        }
        if (command.likes() != null) {
            this.likes = command.likes();
        }
    }

    public void incrementLikes() {
        this.likes++;
    }

    public void decrementLikes() {
        if (this.likes > 0) {
            this.likes--;
        }
    }

    public boolean hasValidRating() {
        return this.rating != null && this.rating >= 1 && this.rating <= 5;
    }
}
