package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.domain.models.Favorite;
import com.geopslabs.geops.engagement.domain.models.queries.GetFavoriteByIdQuery;
import com.geopslabs.geops.engagement.domain.models.queries.GetFavoriteByUserIdAndOfferIdQuery;
import com.geopslabs.geops.engagement.domain.models.queries.GetFavoritesByUserIdQuery;
import com.geopslabs.geops.engagement.application.usecases.FavoriteQueryUseCase;
import com.geopslabs.geops.engagement.domain.ports.FavoriteRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * FavoriteQueryService
 *
 * Implementation of the FavoriteQueryUseCase that handles all query operations
 * for favorites. This service implements the business logic for
 * retrieving and searching favorites following DDD principles.
 *
 * @summary Implementation of favorite query service operations
 * @since 1.0
 * @author GeOps Labs
 */
@Transactional(readOnly = true)
public class FavoriteQueryService implements FavoriteQueryUseCase {

    private final FavoriteRepositoryPort favoriteRepository;

    /**
     * Constructor for dependency injection
     *
     * @param favoriteRepository The repository for favorite data access
     */
    public FavoriteQueryService(FavoriteRepositoryPort favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Favorite> handle(GetFavoritesByUserIdQuery query) {
        try {
            return favoriteRepository.findByUserId(Long.valueOf(query.userId()));
        } catch (Exception e) {
            // Log the error
            System.err.println("Error retrieving favorites by user ID: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Favorite> handle(GetFavoriteByUserIdAndOfferIdQuery query) {
        try {
            return favoriteRepository.findByUserIdAndOfferId(
                query.userId(),
                query.offerId()
            );
        } catch (Exception e) {
            // Log the error
            System.err.println("Error retrieving favorite by userId and offerId: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Favorite> handle(GetFavoriteByIdQuery query) {
        try {
            return favoriteRepository.findById(query.id());
        } catch (Exception e) {
            // Log the error
            System.err.println("Error retrieving favorite by ID: " + e.getMessage());
            return Optional.empty();
        }
    }
}


