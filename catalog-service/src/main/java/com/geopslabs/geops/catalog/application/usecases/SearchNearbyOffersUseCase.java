package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.queries.SearchNearbyOffersQuery;

public interface SearchNearbyOffersUseCase {
    NearbyOffersPage searchNearbyOffers(SearchNearbyOffersQuery query);
}
