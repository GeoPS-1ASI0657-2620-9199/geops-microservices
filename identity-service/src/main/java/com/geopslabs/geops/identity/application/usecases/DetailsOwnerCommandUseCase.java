package com.geopslabs.geops.identity.application.usecases;

import com.geopslabs.geops.identity.domain.models.DetailsOwner;

import java.util.Optional;

/**
 * DetailsOwnerCommandUseCase
 *
 * Service interface for handling owner details command operations
 * This service defines methods for creating and updating owner details
 * following the DDD pattern
 *
 * @summary Service for owner details command operations
 * @since 1.0
 * @author GeOps Labs
 */
public interface DetailsOwnerCommandUseCase {

    /**
     * Handles the command to create new owner details
     *
     * @param command The CreateDetailsOwnerCommand containing owner details data
     * @return An Optional containing the created owner details if successful, empty otherwise
     */
    Optional<DetailsOwner> handle(CreateDetailsOwnerCommand command);

    /**
     * Handles the command to update existing owner details
     *
     * @param command The UpdateDetailsOwnerCommand containing updated owner details data
     * @return An Optional containing the updated owner details if successful, empty otherwise
     */
    Optional<DetailsOwner> handle(UpdateDetailsOwnerCommand command);
}

