package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.Review;
import com.geopslabs.geops.engagement.domain.models.commands.CreateReviewCommand;

import java.util.Optional;

public interface ReviewCommandUseCase {
    Optional<Review> handle(CreateReviewCommand command);
}
