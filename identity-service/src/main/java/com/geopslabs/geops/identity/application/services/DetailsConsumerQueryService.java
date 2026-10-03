package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.domain.models.DetailsConsumer;
import com.geopslabs.geops.identity.application.usecases.GetDetailsConsumerByUserIdQuery;
import com.geopslabs.geops.identity.application.usecases.DetailsConsumerQueryUseCase;
import com.geopslabs.geops.identity.infrastructure.persistence.DetailsConsumerJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * DetailsConsumerQueryService
 *
 * Implementation of the DetailsConsumerQueryUseCase that handles all query operations
 * for consumer details. This service implements the business logic for retrieving
 * consumer details following DDD principles
 *
 * @summary Implementation of consumer details query service operations
 * @since 1.0
 * @author GeOps Labs
 */
@Service
@Transactional(readOnly = true)
public class DetailsConsumerQueryService implements DetailsConsumerQueryUseCase {

    private final DetailsConsumerJpaRepository detailsConsumerRepository;

    /**
     * Constructor for dependency injection
     *
     * @param detailsConsumerRepository The repository for consumer details data access
     */
    public DetailsConsumerQueryService(DetailsConsumerJpaRepository detailsConsumerRepository) {
        this.detailsConsumerRepository = detailsConsumerRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<DetailsConsumer> handle(GetDetailsConsumerByUserIdQuery query) {
        try {
            return detailsConsumerRepository.findByUserId(query.userId());
        } catch (Exception e) {
            System.err.println("Error retrieving consumer details by user ID: " + e.getMessage());
            return Optional.empty();
        }
    }
}

