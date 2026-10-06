package com.geopslabs.geops.engagement.domain.models;

import com.geopslabs.geops.engagement.domain.models.commands.CreateReviewCommand;

import java.time.LocalDateTime;

public class Review {
    private Long id;
    private Long reservationId;
    private Long consumerId;
    private Long businessId;
    private Integer rating;
    private String text;
    private Boolean verifiedRedemption;
    private LocalDateTime createdAt;

    public Review(CreateReviewCommand command) {
        this.reservationId = command.reservationId();
        this.consumerId = command.consumerId();
        this.businessId = command.businessId();
        this.rating = command.rating();
        this.text = command.text();
        this.verifiedRedemption = Boolean.FALSE;
    }

    public Review(Long id, Long reservationId, Long consumerId, Long businessId, Integer rating, String text,
                  Boolean verifiedRedemption, LocalDateTime createdAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.consumerId = consumerId;
        this.businessId = businessId;
        this.rating = rating;
        this.text = text;
        this.verifiedRedemption = verifiedRedemption;
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

    public Integer getRating() {
        return rating;
    }

    public String getText() {
        return text;
    }

    public Boolean getVerifiedRedemption() {
        return verifiedRedemption;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
