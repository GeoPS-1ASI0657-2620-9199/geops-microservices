package com.geopslabs.geops.engagement.infrastructure.web;

public record CreateReviewResource(
    Long reservationId,
    Long consumerId,
    Long businessId,
    Integer rating,
    String text
) {
    public CreateReviewResource {
        if (reservationId == null) {
            throw new IllegalArgumentException("reservationId cannot be null");
        }

        if (consumerId == null) {
            throw new IllegalArgumentException("consumerId cannot be null");
        }

        if (businessId == null) {
            throw new IllegalArgumentException("businessId cannot be null");
        }

        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text cannot be null or empty");
        }

        if (text.length() > 2000) {
            throw new IllegalArgumentException("text cannot exceed 2000 characters");
        }
    }
}
