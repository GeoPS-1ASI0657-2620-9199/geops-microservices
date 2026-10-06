package com.geopslabs.geops.engagement.domain.ports;

import com.geopslabs.geops.engagement.domain.models.Review;

import java.util.List;

public interface ReviewRepositoryPort {
    Review save(Review review);

    List<Review> findByBusinessId(Long businessId);
}
