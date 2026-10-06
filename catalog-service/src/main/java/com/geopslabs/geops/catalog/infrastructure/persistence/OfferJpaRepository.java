package com.geopslabs.geops.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * OfferJpaRepository
 * JPA Repository interface for Offer aggregate root
 * This repository provides data access operations for offers
 * including custom queries for offer management and retrieval operations
 *
 * @summary JPA Repository for offer data access operations
 * @since 1.0
 * @author GeOps Labs
 */
@Repository
public interface OfferJpaRepository extends JpaRepository<OfferJpaEntity, Long> {

    /**
     * Finds all offers by a list of IDs
     *
     * @param ids The list of offer IDs to retrieve
     * @return A List of Offer objects with the specified IDs
     */
    List<OfferJpaEntity> findByIdIn(List<Long> ids);

    /**
     * Finds all offers from a campaign using its campaign unique id
     * @param campaignId The campaign ID
     * @return A list of offer objects from the campaign
     */
    List<OfferJpaEntity> findByCampaign_Id(Long campaignId);

}
