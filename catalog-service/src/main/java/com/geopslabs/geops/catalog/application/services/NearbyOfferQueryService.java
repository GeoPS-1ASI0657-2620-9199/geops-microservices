package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.NearbyOffersPage;
import com.geopslabs.geops.catalog.application.usecases.RecordOfferInteractionsUseCase;
import com.geopslabs.geops.catalog.application.usecases.SearchNearbyOffersUseCase;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.OfferCategory;
import com.geopslabs.geops.catalog.domain.models.ProximitySearchService;
import com.geopslabs.geops.catalog.domain.models.SearchText;
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
    private final RecordOfferInteractionsUseCase interactions;

    public NearbyOfferQueryService(ProximitySearchService proximitySearch) {
        this(proximitySearch, RecordOfferInteractionsUseCase.NONE);
    }

    /** Every offer of the page returned counts as one impression of its campaign (US11). */
    public NearbyOfferQueryService(ProximitySearchService proximitySearch,
                                   RecordOfferInteractionsUseCase interactions) {
        this.proximitySearch = proximitySearch;
        this.interactions = interactions;
    }

    @Override
    public NearbyOffersPage searchNearbyOffers(SearchNearbyOffersQuery query) {
        var radius = new WalkingRadius(query.radiusMinutes());
        var origin = new GeoPoint(query.latitude(), query.longitude());
        var category = OfferCategory.labelOrNull(query.category());
        var text = SearchText.parseOrNull(query.text());
        var page = query.page();
        var size = query.size();
        if (page < FIRST_PAGE || size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new InvalidPageRequestException(MAX_PAGE_SIZE);
        }

        var ranked = proximitySearch.search(origin, radius.toMeters(), category, text);
        var from = (int) Math.min((long) page * size, ranked.size());
        var to = Math.min(from + size, ranked.size());
        var totalPages = (ranked.size() + size - 1) / size;
        LOGGER.info("offers.nearby.searched radiusMinutes={} category={} hasText={} page={} found={}", radius.minutes(),
                category, text != null, page, ranked.size());
        var content = ranked.subList(from, to);
        interactions.recordImpressions(content.stream().map(offer -> offer.offer().offerId()).toList());
        return new NearbyOffersPage(content, page, ranked.size(), totalPages);
    }
}
