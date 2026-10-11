package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.queries.SearchNearbyOffersQuery;

/**
 * E2-08 and CA-39.2: the offers of public sources near the consumer, listed apart from the affiliated ones.
 */
public interface SearchPublicSourceOffersUseCase {
    PublicSourceOffersPage searchPublicSources(SearchNearbyOffersQuery query);
}
