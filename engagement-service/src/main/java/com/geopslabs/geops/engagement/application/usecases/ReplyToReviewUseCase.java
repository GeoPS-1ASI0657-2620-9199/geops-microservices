package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.ReviewReply;
public interface ReplyToReviewUseCase {
    ReviewReply reply(Long reviewId, Long businessId, String text);
}
