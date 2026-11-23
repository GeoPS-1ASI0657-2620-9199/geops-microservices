package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.OfferSnapshot;
import java.time.Instant;
import java.util.List;

/** A saved business with every offer it has open today, so the consumer picks one instead of being sent to the first (US12). */
public record SavedBusinessView(Long id, Long businessId, String businessName, Instant savedAt,
                                int activeOffersCount, List<OfferSnapshot> activeOffers) {}
