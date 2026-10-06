package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.application.usecases.ReviewCommandUseCase;
import com.geopslabs.geops.engagement.domain.models.Review;
import com.geopslabs.geops.engagement.domain.models.commands.CreateReviewCommand;
import com.geopslabs.geops.engagement.domain.ports.ReviewRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ReviewCommandService implements ReviewCommandUseCase {
    private final ReviewRepositoryPort reviewRepository;

    public ReviewCommandService(ReviewRepositoryPort reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    public Review handle(CreateReviewCommand command) {
        return reviewRepository.save(new Review(command));
    }
}
