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
     * Finds all reviews for a specific offer
     *
     * @param offerId The unique identifier of the offer
     * @return A List of Review objects for the specified offer
     */
    List<ReviewJpaEntity> findByOffer_Id(Long offerId);

    /**
     * Finds all reviews by a specific user
     *
     * @param userId The unique identifier of the user
     * @return A List of Review objects created by the specified user
     */
    List<ReviewJpaEntity> findByUser_Id(Long userId);

    /**
     * Finds all reviews for an offer ordered by creation date (most recent first)
     *
     * @param offerId The unique identifier of the offer
     * @return A List of Review objects ordered by creation date descending
     */
    List<ReviewJpaEntity> findByOffer_IdOrderByCreatedAtDesc(Long offerId);

    /**
     * Finds all reviews for an offer ordered by likes (most liked first)
     *
     * @param offerId The unique identifier of the offer
     * @return A List of Review objects ordered by likes descending
     */
    List<ReviewJpaEntity> findByOffer_IdOrderByLikesDesc(Long offerId);

    /**
     * Counts the total number of reviews for a specific offer.
     *
     * @param offerId The unique identifier of the offer
     * @return The number of reviews for the offer
     */
    long countByOffer_Id(Long offerId);
}
