package com.geopslabs.geops.engagement.infrastructure.web;

import java.time.Instant;

public record ReviewResponse(
        Long reviewId,
        Long businessId,
        Integer rating,
        String text,
        boolean verifiedRedemption,
        Instant createdAt) {
}
