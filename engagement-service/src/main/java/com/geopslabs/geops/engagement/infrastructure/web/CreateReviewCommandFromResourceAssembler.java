package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.commands.CreateReviewCommand;

public final class CreateReviewCommandFromResourceAssembler {

    private CreateReviewCommandFromResourceAssembler() {
    }

    public static CreateReviewCommand toCommand(AuthenticatedUser user, CreateReviewResource resource) {
        return new CreateReviewCommand(resource.reservationId(), user.consumerId(), resource.businessId(),
                resource.rating(), resource.text());
    }
}
