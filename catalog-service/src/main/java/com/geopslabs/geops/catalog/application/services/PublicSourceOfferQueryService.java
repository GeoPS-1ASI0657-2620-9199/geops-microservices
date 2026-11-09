package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.PublicSourceOffersPage;
import com.geopslabs.geops.catalog.application.usecases.SearchPublicSourceOffersUseCase;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.OfferCategory;
import com.geopslabs.geops.catalog.domain.models.WalkingRadius;
import com.geopslabs.geops.catalog.domain.models.queries.SearchNearbyOffersQuery;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;

/**
 * Public source offers within the radius, ordered by distance (E2-08). They have no campaign, so they never enter
 * the cell cache, the zone rule or the ranking by reports and seal.
 */
public class PublicSourceOfferQueryService implements SearchPublicSourceOffersUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicSourceOfferQueryService.class);
    private static final int FIRST_PAGE = 0;
    private static final int MIN_PAGE_SIZE = 1;

    private final OfferRepositoryPort offers;
    private final Clock clock;

    public PublicSourceOfferQueryService(OfferRepositoryPort offers, Clock clock) {
        this.offers = offers;
        this.clock = clock;
    }

    @Override
    public PublicSourceOffersPage searchPublicSources(SearchNearbyOffersQuery query) {
        var radius = new WalkingRadius(query.radiusMinutes());
        var origin = new GeoPoint(query.latitude(), query.longitude());
        var category = OfferCategory.labelOrNull(query.category());
        var page = query.page();
        var size = query.size();
        if (page < FIRST_PAGE || size < MIN_PAGE_SIZE || size > NearbyOfferQueryService.MAX_PAGE_SIZE) {
            throw new InvalidPageRequestException(NearbyOfferQueryService.MAX_PAGE_SIZE);
        }
        var meters = radius.toMeters();
        var ranked = offers.findPublicSourceWithin(origin, meters, LocalDate.now(clock)).stream()
                .filter(offer -> category == null || category.equals(offer.category()))
                .map(offer -> {
                    var distance = origin.distanceTo(offer.location());
                    return new PublicSourceOffersPage.Ranked(offer, distance, WalkingRadius.walkMinutesFor(distance));
                })
                .filter(offer -> offer.distanceMeters() <= meters)
                .sorted(Comparator.comparingDouble(PublicSourceOffersPage.Ranked::distanceMeters))
                .toList();
        var from = (int) Math.min((long) page * size, ranked.size());
        var to = Math.min(from + size, ranked.size());
        LOGGER.info("offers.public-sources.searched radiusMinutes={} category={} found={}", radius.minutes(),
                category, ranked.size());
        return new PublicSourceOffersPage(ranked.subList(from, to), page, ranked.size(),
                (ranked.size() + size - 1) / size);
    }
}
