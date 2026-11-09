package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.PublicSourceOffersPage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * BRD-08, 7.3: sourceName instead of businessId and no verifiedSeal, so the web cannot show them as verified.
 */
public record PublicSourceOffersResponse(List<Item> content, int page, long totalElements, int totalPages) {

    static PublicSourceOffersResponse from(PublicSourceOffersPage page) {
        var items = page.content().stream().map(ranked -> {
            var offer = ranked.offer();
            return new Item(offer.offerId(), offer.title(), offer.sourceName(), Math.round(ranked.distanceMeters()),
                    ranked.walkMinutes(), offer.category(), offer.address(), offer.imageUrl(),
                    offer.location().latitude(), offer.location().longitude(), offer.price(), offer.validTo());
        }).toList();
        return new PublicSourceOffersResponse(items, page.page(), page.totalElements(), page.totalPages());
    }

    public record Item(Long offerId, String title, String sourceName, long distanceMeters, int walkMinutes,
                       String category, String address, String imageUrl, double latitude, double longitude,
                       BigDecimal price, LocalDate validTo) {
    }
}
