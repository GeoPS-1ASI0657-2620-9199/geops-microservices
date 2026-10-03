package com.geopslabs.geops.identity.application.usecases;

import com.geopslabs.geops.identity.domain.models.BusinessProfile;

import java.util.Optional;

/**
 * DetailsOwnerQueryUseCase
 *
 * Service interface for handling owner details query operations
 * This service defines methods for retrieving owner details information
 * following the DDD pattern
 *
 * @summary Service for owner details query operations
 * @since 1.0
 * @author GeOps Labs
 */
public interface DetailsOwnerQueryUseCase {

    /**
     * Handles the query to get owner details by user ID
     *
     * @param query The GetDetailsOwnerByUserIdQuery containing the user ID
     * @return An Optional containing the owner details if found, empty otherwise
     */
    Optional<BusinessProfile> handle(GetDetailsOwnerByUserIdQuery query);
}

