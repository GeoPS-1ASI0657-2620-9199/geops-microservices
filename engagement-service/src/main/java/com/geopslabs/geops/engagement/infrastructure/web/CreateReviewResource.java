package com.geopslabs.geops.engagement.infrastructure.web;

/**
 * CreateReviewResource
 *
 * Resource representing the data required to create a new review for an offer by a user
 *
 * @summary Resource for creating a review
 * @since 1.0
 * @author GeOps Labs
 */
public record CreateReviewResource(
    Long reservationId,
    Long consumerId,
    Long businessId,
    String userName,
    Integer rating,
    String text
) {
    /**
     * Compact constructor that validates the resource parameters
     *
     * @throws IllegalArgumentException if validation fails
     */
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

        if (userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("userName cannot be null or empty");
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
