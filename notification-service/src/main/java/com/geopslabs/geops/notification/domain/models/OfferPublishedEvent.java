package com.geopslabs.geops.notification.domain.models;

import java.time.*;
public record OfferPublishedEvent(Long offerId, Long businessId, String businessName, String title,
                                  LocalDate validTo, GeoPoint location, Zone zone, Instant occurredAt) {
    public record Zone(GeoPoint center, int reachMeters) {}
}
