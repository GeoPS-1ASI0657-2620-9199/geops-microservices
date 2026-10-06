package com.geopslabs.geops.engagement.domain.models;

import java.time.LocalDate;
import java.util.Set;

public record OfferSnapshot(Long offerId, Long businessId, String title, LocalDate validTo, String status) {
    public static final String PUBLISHED = "PUBLISHED";
    public static final String EXPIRED = "EXPIRED";
    public static final String REMOVED = "REMOVED";
    private static final Set<String> ENDED_STATUSES = Set.of(EXPIRED, REMOVED);

    public boolean isExpired(LocalDate today) {
        return validTo.isBefore(today) || ENDED_STATUSES.contains(status);
    }
}
