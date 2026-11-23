package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.ModerationAction;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;

import java.time.Instant;

public record ModerationActionResponse(Long moderationId, Long offerId, OfferStatus offerStatus, Long adminId,
                                       String reason, Instant removedAt) {

    static ModerationActionResponse from(ModerationAction action) {
        return new ModerationActionResponse(action.id(), action.offerId(), OfferStatus.REMOVED, action.adminId(),
                action.reason(), action.removedAt());
    }
}
