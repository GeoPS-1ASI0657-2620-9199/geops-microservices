package com.geopslabs.geops.engagement.domain.models.commands;

/**
 * CreateReviewCommand
 *
 * Command to create a new review for an offer
 * Contains all necessary data for review creation with validations
 *
 * @summary Command to create a new review
 * @param reservationId The ID of the redeemed reservation behind the review
 * @param consumerId The ID of the consumer creating the review
 * @param businessId The ID of the business being reviewed
 * @param userName The name of the user creating the review
 * @param rating   The rating given (1-5)
 * @param text     The review text/comment
 *
 * @since 1.0
 * @author GeOps Labs
 */
public record CreateReviewCommand(
        Long reservationId,
        Long consumerId,
        Long businessId,
        String userName,
        Integer rating,
        String text
) {
    /**
     * Compact constructor that validates the command parameters
     *
     * @throws IllegalArgumentException if validation fails
     */
    public CreateReviewCommand {
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
