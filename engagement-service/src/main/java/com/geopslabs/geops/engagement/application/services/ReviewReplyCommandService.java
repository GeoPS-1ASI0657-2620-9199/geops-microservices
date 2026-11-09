package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.application.usecases.ReplyToReviewUseCase;
import com.geopslabs.geops.engagement.domain.models.ReviewReply;
import com.geopslabs.geops.engagement.domain.models.exceptions.ReviewNotFoundException;
import com.geopslabs.geops.engagement.domain.ports.ReviewRepositoryPort;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

public class ReviewReplyCommandService implements ReplyToReviewUseCase {
    private final ReviewRepositoryPort reviews;
    private final Clock clock;

    public ReviewReplyCommandService(ReviewRepositoryPort reviews, Clock clock) {
        this.reviews = reviews;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ReviewReply reply(Long reviewId, Long businessId, String text) {
        var review = reviews.findById(reviewId).orElseThrow(ReviewNotFoundException::new);
        return reviews.saveReply(reviewId, businessId, review.reply(businessId, text, clock.instant()));
    }
}
