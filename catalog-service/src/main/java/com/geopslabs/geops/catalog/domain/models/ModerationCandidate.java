package com.geopslabs.geops.catalog.domain.models;

import java.time.LocalDate;

/**
 * An offer as the administrator sees it in the moderation panel (US24): its status and the business behind it with
 * its open reports. businessName is the source name for a public source offer.
 */
public record ModerationCandidate(Long offerId, String title, String category, OfferSource source, Long businessId,
                                  String businessName, OfferStatus status, LocalDate validTo, int openReports,
                                  boolean verifiedSeal) {
}
