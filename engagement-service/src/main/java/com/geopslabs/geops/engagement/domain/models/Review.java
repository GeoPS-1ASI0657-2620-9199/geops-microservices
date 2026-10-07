package com.geopslabs.geops.engagement.domain.models;

import java.time.LocalDateTime;

public class Review {
    private final Long id;
    private final Long consumerId;
    private final Long businessId;
    private final Long reservationId;
    private final Rating rating;
    private final String text;
    private final Boolean verifiedRedemption;
    private final LocalDateTime createdAt;

    @SuppressWarnings("java:S107")
    public Review(Long id, Long consumerId, Long businessId, Long reservationId, Rating rating, String text,
                  Boolean verifiedRedemption, LocalDateTime createdAt) {
        this.id = id;
        this.consumerId = consumerId;
        this.businessId = businessId;
        this.reservationId = reservationId;
        this.rating = rating;
        this.text = text;
        this.verifiedRedemption = verifiedRedemption;
        this.createdAt = createdAt;
    }

    public static Review of(RedeemedReservation redemption, Rating rating, String text, LocalDateTime createdAt) {
        return new Review(null, redemption.consumerId(), redemption.businessId(), redemption.reservationId(), rating,
                text, Boolean.TRUE, createdAt);
    }

    public Long getId() {
        return id;
    }

    public Long getConsumerId() {
        return consumerId;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public Rating getRating() {
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
