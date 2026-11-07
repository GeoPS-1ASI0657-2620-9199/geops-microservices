package com.geopslabs.geops.catalog.domain.models;

import java.time.LocalDate;

public record ExpiredOffer(Long offerId, Long campaignId, Long businessId, LocalDate validTo, GeoPoint location) {
    public boolean belongsToCampaign() {
        return campaignId != null;
    }

    public boolean hasLocation() {
        return location != null;
    }
}
