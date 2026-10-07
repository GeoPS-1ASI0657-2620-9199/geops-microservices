package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.Review;

import java.time.ZoneOffset;

public final class ReviewResponseAssembler {

    private ReviewResponseAssembler() {
    }

    public static ReviewResponse toResponse(Review review) {
        return new ReviewResponse(review.getId(), review.getBusinessId(), review.getRating().stars(), review.getText(),
                review.getVerifiedRedemption(), review.getCreatedAt().toInstant(ZoneOffset.UTC));
    }
}
