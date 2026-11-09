package com.geopslabs.geops.engagement.domain.models;

import java.time.Instant;

public record Follow(Long id, Long consumerId, Long businessId, Instant followedAt) {
    public static Follow start(Long consumerId, Long businessId, Instant now) {
        return new Follow(null, consumerId, businessId, now);
    }
}
