package com.geopslabs.geops.engagement.infrastructure.web;

import java.time.Instant;
import java.time.LocalDate;

public record SavedOfferResponse(
        Long savedOfferId,
        Long offerId,
        Long businessId,
        String businessName,
        String title,
        LocalDate validTo,
        boolean expired,
        Instant savedAt) {
}
