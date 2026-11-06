package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.*;
import java.time.Instant;
import java.util.Collection;

public interface ProjectCatalogEventsUseCase {
    void offerPublished(BusinessSnapshot business, OfferSnapshot offer, Instant occurredAt);
    void campaignPaused(Collection<Long> offerIds, Instant occurredAt);
    void offerExpired(Long offerId, Instant occurredAt);
}
