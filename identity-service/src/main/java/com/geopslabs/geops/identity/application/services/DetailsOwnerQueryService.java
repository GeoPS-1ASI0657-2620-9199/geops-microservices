package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.domain.models.DetailsOwner;
import com.geopslabs.geops.identity.application.usecases.GetDetailsOwnerByUserIdQuery;
import com.geopslabs.geops.identity.application.usecases.DetailsOwnerQueryUseCase;
import com.geopslabs.geops.identity.infrastructure.persistence.DetailsOwnerJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * DetailsOwnerQueryService
 *
 * Implementation of the DetailsOwnerQueryUseCase that handles all query operations
 * for owner details. This service implements the business logic for retrieving
 * owner details following DDD principles
 *
 * @summary Implementation of owner details query service operations
 * @since 1.0
 * @author GeOps Labs
 */
@Service
@Transactional(readOnly = true)
public class DetailsOwnerQueryService implements DetailsOwnerQueryUseCase {

    private final DetailsOwnerJpaRepository detailsOwnerRepository;

    /**
     * Constructor for dependency injection
     *
     * @param detailsOwnerRepository The repository for owner details data access
     */
    public DetailsOwnerQueryService(DetailsOwnerJpaRepository detailsOwnerRepository) {
        this.detailsOwnerRepository = detailsOwnerRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<DetailsOwner> handle(GetDetailsOwnerByUserIdQuery query) {
        try {
            return detailsOwnerRepository.findByUserId(query.userId());
        } catch (Exception e) {
            System.err.println("Error retrieving owner details by user ID: " + e.getMessage());
            return Optional.empty();
        }
    }
}

