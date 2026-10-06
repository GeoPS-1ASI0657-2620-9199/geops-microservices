package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.Review;
import com.geopslabs.geops.engagement.infrastructure.web.ReviewResource;

public class ReviewResourceFromEntityAssembler {
    public static ReviewResource toResourceFromEntity(Review entity) {
        return new ReviewResource(
            entity.getId(),
            entity.getReservationId(),
            entity.getConsumerId(),
            entity.getBusinessId(),
            entity.getRating(),
            entity.getText(),
            entity.getVerifiedRedemption(),
            entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null
        );
    }
}
