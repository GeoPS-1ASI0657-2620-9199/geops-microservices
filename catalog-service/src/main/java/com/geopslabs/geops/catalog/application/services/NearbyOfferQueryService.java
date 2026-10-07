package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.NearbyOffersPage;
import com.geopslabs.geops.catalog.application.usecases.SearchNearbyOffersUseCase;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.ProximitySearchService;
import com.geopslabs.geops.catalog.domain.models.WalkingRadius;
import com.geopslabs.geops.catalog.domain.models.queries.SearchNearbyOffersQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NearbyOfferQueryService implements SearchNearbyOffersUseCase {
    public static final int MAX_PAGE_SIZE = 20;
    private static final int FIRST_PAGE = 0;
    private static final int MIN_PAGE_SIZE = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(NearbyOfferQueryService.class);

    private final ProximitySearchService proximitySearch;

    public NearbyOfferQueryService(ProximitySearchService proximitySearch) {
        this.proximitySearch = proximitySearch;
    }

    @Override
    public NearbyOffersPage searchNearbyOffers(SearchNearbyOffersQuery query) {
        var radius = new WalkingRadius(query.radiusMinutes());
        var origin = new GeoPoint(query.latitude(), query.longitude());
        var page = query.page();
        var size = query.size();
        if (page < FIRST_PAGE || size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new InvalidPageRequestException(MAX_PAGE_SIZE);
        }

        var ranked = proximitySearch.search(origin, radius.toMeters(), null);
        var from = (int) Math.min((long) page * size, ranked.size());
        var to = Math.min(from + size, ranked.size());
        var totalPages = (ranked.size() + size - 1) / size;
        LOGGER.info("offers.nearby.searched radiusMinutes={} page={} found={}", radius.minutes(), page, ranked.size());
        return new NearbyOffersPage(ranked.subList(from, to), page, ranked.size(), totalPages);
    }
}
