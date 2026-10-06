package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.Review;
import com.geopslabs.geops.engagement.domain.models.queries.GetReviewsByBusinessQuery;

import java.util.List;

public interface ReviewQueryUseCase {

    List<Review> handle(GetReviewsByBusinessQuery query);
}

