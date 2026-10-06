package com.geopslabs.geops.engagement.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ReviewRepository
 *
 * JPA Repository interface for Review aggregate root
 * This repository provides data access operations for reviews
 *
 * @summary JPA Repository for review data access operations
 * @since 1.0
 * @author GeOps Labs
 */
@Repository
public interface ReviewJpaRepository extends JpaRepository<ReviewJpaEntity, Long> {

    /**
     * Finds all reviews for a specific business
     *
     * @param businessId The unique identifier of the business
     * @return A List of Review objects for the specified business
     */
    List<ReviewJpaEntity> findByBusinessId(Long businessId);

    /**
     * Finds all reviews by a specific consumer
     *
     * @param consumerId The unique identifier of the consumer
     * @return A List of Review objects created by the specified consumer
     */
    List<ReviewJpaEntity> findByConsumerId(Long consumerId);

    /**
     * Finds all reviews for a business ordered by creation date (most recent first)
     *
     * @param businessId The unique identifier of the business
     * @return A List of Review objects ordered by creation date descending
     */
    List<ReviewJpaEntity> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    /**
     * Finds all reviews for a business ordered by likes (most liked first)
     *
     * @param businessId The unique identifier of the business
     * @return A List of Review objects ordered by likes descending
     */
    List<ReviewJpaEntity> findByBusinessIdOrderByLikesDesc(Long businessId);

    /**
     * Counts the total number of reviews for a specific business.
     *
     * @param businessId The unique identifier of the business
     * @return The number of reviews for the business
     */
    long countByBusinessId(Long businessId);
}
