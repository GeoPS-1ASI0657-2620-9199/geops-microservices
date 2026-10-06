package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.Review;
import com.geopslabs.geops.engagement.domain.models.queries.GetAllReviewsQuery;
import com.geopslabs.geops.engagement.domain.models.queries.GetReviewByIdQuery;
import com.geopslabs.geops.engagement.domain.models.queries.GetReviewsByBusinessQuery;

import java.util.List;
import java.util.Optional;

/**
 * ReviewQueryService
 *
 * Domain service interface that defines query operations for reviews.
 * This service handles all read operations following the Command Query Responsibility
 * Segregation (CQRS) pattern, providing various ways to retrieve review data.
 *
 * @summary Service interface for handling review query operations
 * @since 1.0
 * @author GeOps Labs
 */
public interface ReviewQueryUseCase {

    /**
     * Handles the query to retrieve all reviews.
     *
     * This method processes the query to find all reviews in the system.
     * It's commonly used for displaying the complete review list.
     *
     * @param query The query to retrieve all reviews
     * @return A List containing all Reviews
     * @throws IllegalArgumentException if the query contains invalid data
     */
    List<Review> handle(GetAllReviewsQuery query);

    /**
     * Handles the query to retrieve a review by its unique identifier.
     *
     * This method processes the query to find a specific review using its ID.
     * It's commonly used for review details views and validation.
     *
     * @param query The query containing the review ID
     * @return An Optional containing the Review if found, empty otherwise
     * @throws IllegalArgumentException if the query contains invalid data
     */
    Optional<Review> handle(GetReviewByIdQuery query);

    /**
     * Handles the query to retrieve all reviews for a specific business.
     *
     * This method processes the query to find all reviews associated with a business.
     * Endpoint: GET /api/v1/reviews?businessId={id}
     *
     * @param query The query containing the business ID
     * @return A List containing all Reviews for the specified business
     * @throws IllegalArgumentException if the query contains invalid data
     */
    List<Review> handle(GetReviewsByBusinessQuery query);
}

