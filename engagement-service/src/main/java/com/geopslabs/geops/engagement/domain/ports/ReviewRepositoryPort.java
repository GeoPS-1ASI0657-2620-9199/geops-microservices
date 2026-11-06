package com.geopslabs.geops.engagement.domain.ports;

import com.geopslabs.geops.engagement.domain.models.Review;

import java.util.List;

public interface ReviewRepositoryPort {
    java.util.Optional<Review> findById(Long reviewId);

    com.geopslabs.geops.engagement.domain.models.ReviewReply saveReply(Long reviewId, Long businessId, com.geopslabs.geops.engagement.domain.models.ReviewReply reply);

    Review save(Review review);

    List<Review> findByBusinessId(Long businessId);
}
