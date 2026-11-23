package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;

import java.time.LocalDate;

public record ModerationOfferRow(Long offerId, String title, String category, OfferSource source, Long businessId,
                                 String businessName, OfferStatus status, LocalDate validTo, Integer openReports,
                                 Boolean verifiedSeal) {
}
