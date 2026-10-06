package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.commands.CreateReviewCommand;
import com.geopslabs.geops.engagement.infrastructure.web.CreateReviewResource;

public class CreateReviewCommandFromResourceAssembler {
    public static CreateReviewCommand toCommandFromResource(CreateReviewResource resource) {
        return new CreateReviewCommand(
            resource.reservationId(),
            resource.consumerId(),
            resource.businessId(),
            resource.rating(),
            resource.text()
        );
    }
}
