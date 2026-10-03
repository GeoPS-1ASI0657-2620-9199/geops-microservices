package com.geopslabs.geops.identity.application.usecases;

import com.geopslabs.geops.identity.domain.models.DetailsConsumer;

import java.util.Optional;

/**
 * DetailsConsumerCommandUseCase
 *
 * Service interface for handling consumer details command operations
 * This service defines methods for creating and updating consumer details
 * following the DDD pattern
 *
 * @summary Service for consumer details command operations
 * @since 1.0
 * @author GeOps Labs
 */
public interface DetailsConsumerCommandUseCase {

    /**
     * Handles the command to create new consumer details
     *
     * @param command The CreateDetailsConsumerCommand containing consumer details data
     * @return An Optional containing the created consumer details if successful, empty otherwise
     */
    Optional<DetailsConsumer> handle(CreateDetailsConsumerCommand command);

    /**
     * Handles the command to update existing consumer details
     *
     * @param command The UpdateDetailsConsumerCommand containing updated consumer details data
     * @return An Optional containing the updated consumer details if successful, empty otherwise
     */
    Optional<DetailsConsumer> handle(UpdateDetailsConsumerCommand command);
}

