package com.geopslabs.geops.engagement.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * SavedOfferRepository
 *
 * JPA Repository interface for SavedOffer aggregate root
 * This repository provides data access operations for saved offers
 *
 * @summary JPA Repository for saved offer data access operations
 * @since 1.0
 * @author GeOps Labs
 */
@Repository
public interface SavedOfferJpaRepository extends JpaRepository<SavedOfferJpaEntity, Long> {

    /**
     * Finds all saved offers for a specific user
     *
     * @param userId The unique identifier of the user
     * @return A List of SavedOffer objects for the specified user
     */
    List<SavedOfferJpaEntity> findByUser_Id(Long userId);

    /**
     * Finds a saved offer by user ID and offer ID
     *
     * @param userId The unique identifier of the user
     * @param offerId The unique identifier of the offer
     * @return An Optional containing the SavedOffer if found, empty otherwise
     */
    Optional<SavedOfferJpaEntity> findByUser_IdAndOffer_Id(Long userId, Long offerId);

    /**
     * Checks if a saved offer exists for a specific user and offer
     *
     * @param userId The unique identifier of the user
     * @param offerId The unique identifier of the offer
     * @return true if the saved offer exists, false otherwise
     */
    boolean existsByUser_IdAndOffer_Id(Long userId, Long offerId);

    /**
     * Counts the total number of saved offers for a specific offer
     *
     * @param offerId The unique identifier of the offer
     * @return The number of users who saved offers this offer
     */
    long countByOffer_Id(Long offerId);

    /**
     * Deletes all saved offers for a specific offer
     * Useful for cascade deletion when an offer is removed
     *
     * @param offerId The unique identifier of the offer
     * @return The number of deleted saved offers
     */
    long deleteByOffer_Id(Long offerId);

    /**
     * Deletes a saved offer by user ID and offer ID
     * Useful for removing a specific saved offer relationship
     *
     * @param userId The unique identifier of the user
     * @param offerId The unique identifier of the offer
     * @return The number of deleted saved offers (0 or 1)
     */
    long deleteByUser_IdAndOffer_Id(Long userId, Long offerId);
}

