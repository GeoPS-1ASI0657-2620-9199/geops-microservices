package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.application.usecases.ReviewQueryUseCase;
import com.geopslabs.geops.engagement.domain.models.Review;
import com.geopslabs.geops.engagement.domain.models.queries.GetReviewsByBusinessQuery;
import com.geopslabs.geops.engagement.domain.ports.ReviewRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
public class ReviewQueryService implements ReviewQueryUseCase {
    private final ReviewRepositoryPort reviewRepository;

    public ReviewQueryService(ReviewRepositoryPort reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    public List<Review> handle(GetReviewsByBusinessQuery query) {
        return reviewRepository.findByBusinessIdOrderByCreatedAtDesc(query.businessId());
    }
}
