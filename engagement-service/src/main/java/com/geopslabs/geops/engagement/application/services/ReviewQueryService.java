package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.application.usecases.ListBusinessReviewsUseCase;
import com.geopslabs.geops.engagement.domain.models.Review;
import com.geopslabs.geops.engagement.domain.models.queries.GetReviewsByBusinessQuery;
import com.geopslabs.geops.engagement.domain.ports.ReviewRepositoryPort;

import java.util.List;

public class ReviewQueryService implements ListBusinessReviewsUseCase {
    private final ReviewRepositoryPort reviewRepository;

    public ReviewQueryService(ReviewRepositoryPort reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    public List<Review> list(GetReviewsByBusinessQuery query) {
        return reviewRepository.findByBusinessId(query.businessId());
    }
}
