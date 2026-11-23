package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.ModerationCandidate;
import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import com.geopslabs.geops.catalog.domain.models.ResultPage;

import java.time.LocalDate;
import java.util.List;

public record ModerationOffersResponse(List<Item> content, int page, long totalElements, int totalPages) {

    static ModerationOffersResponse from(ResultPage<ModerationCandidate> page) {
        var items = page.content().stream().map(offer -> new Item(offer.offerId(), offer.title(), offer.category(),
                offer.source(), offer.businessId(), offer.businessName(), offer.status(), offer.validTo(),
                offer.openReports(), offer.verifiedSeal())).toList();
        return new ModerationOffersResponse(items, page.page(), page.totalElements(), page.totalPages());
    }

    public record Item(Long offerId, String title, String category, OfferSource source, Long businessId,
                       String businessName, OfferStatus status, LocalDate validTo, int openReports,
                       boolean verifiedSeal) {
    }
}
