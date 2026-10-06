package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.domain.models.Review;
import com.geopslabs.geops.engagement.domain.models.queries.GetAllReviewsQuery;
import com.geopslabs.geops.engagement.domain.models.queries.GetReviewByIdQuery;
import com.geopslabs.geops.engagement.domain.models.queries.GetReviewsByOfferIdQuery;
import com.geopslabs.geops.engagement.application.usecases.ReviewQueryUseCase;
import com.geopslabs.geops.engagement.domain.ports.ReviewRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ReviewQueryService
 *
 * Implementation of the ReviewQueryUseCase that handles all query operations
 * for reviews. This service implements the business logic for
 * retrieving and searching reviews following DDD principles
 *
 * @summary Implementation of review query service operations
 * @since 1.0
 * @author GeOps Labs
 */
@Transactional(readOnly = true)
public class ReviewQueryService implements ReviewQueryUseCase {

    private final ReviewRepositoryPort reviewRepository;

    /**
     * Constructor for dependency injection
     *
     * @param reviewRepository The repository for review data access
     */
    public ReviewQueryService(ReviewRepositoryPort reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Review> handle(GetAllReviewsQuery query) {
        try {
            return reviewRepository.findAll();
        } catch (Exception e) {
            // Log the error
            System.err.println("Error retrieving all reviews: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Review> handle(GetReviewByIdQuery query) {
        try {
            return reviewRepository.findById(query.id());
        } catch (Exception e) {
            // Log the error
            System.err.println("Error retrieving review by ID: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Review> handle(GetReviewsByOfferIdQuery query) {
        try {
            return reviewRepository.findByOfferIdOrderByCreatedAtDesc(query.offerId());
        } catch (Exception e) {
            // Log the error
            System.err.println("Error retrieving reviews by offer ID: " + e.getMessage());
            return List.of();
        }
    }
}


