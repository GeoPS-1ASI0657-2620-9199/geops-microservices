package com.geopslabs.geops.catalog.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NearbyOfferCandidate(
        Long offerId,
        String title,
        BigDecimal price,
        LocalDate validTo,
        String category,
        GeoPoint location,
        Long businessId,
        String businessName,
        boolean verifiedSeal,
        int openReports,
        CampaignZone zone) {

    public boolean hasOpenReports() {
        return openReports > 0;
    }
}
