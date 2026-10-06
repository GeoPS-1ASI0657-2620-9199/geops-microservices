package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.Review;
import com.geopslabs.geops.engagement.domain.models.commands.CreateReviewCommand;

public interface ReviewCommandUseCase {
    Review handle(CreateReviewCommand command);
}
