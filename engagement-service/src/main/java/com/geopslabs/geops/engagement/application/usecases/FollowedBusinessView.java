package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.OfferSnapshot;
import java.time.Instant;
import java.util.List;

public record FollowedBusinessView(Long businessId, String businessName, Instant followedAt,
                                 int activeOffersCount, List<OfferSnapshot> activeOffers) {}
