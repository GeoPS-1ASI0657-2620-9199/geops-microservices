package com.geopslabs.geops.engagement.application.services;
import com.geopslabs.geops.engagement.domain.models.Review;
import com.geopslabs.geops.engagement.domain.models.commands.CreateReviewCommand;
import com.geopslabs.geops.engagement.application.usecases.ReviewCommandUseCase;
import com.geopslabs.geops.engagement.domain.ports.ReviewRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
public class ReviewCommandService implements ReviewCommandUseCase {
    private final ReviewRepositoryPort reviewRepository;

    public ReviewCommandService(ReviewRepositoryPort reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    public Optional<Review> handle(CreateReviewCommand command) {
        try {
            var review = new Review(command);

            var savedReview = reviewRepository.save(review);

            return Optional.of(savedReview);

        } catch (Exception e) {
            System.err.println("Error creating review: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
