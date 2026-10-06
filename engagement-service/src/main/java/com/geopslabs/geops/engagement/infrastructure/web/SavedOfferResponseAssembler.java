package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.infrastructure.web.SavedOfferResponse;

public class SavedOfferResponseAssembler {
    public static SavedOfferResponse toResourceFromEntity(SavedOffer entity) {
        return new SavedOfferResponse(
                entity.getId(),
                entity.getConsumerId(),
                entity.getOfferId(),
                entity.getSavedAt() != null ? entity.getSavedAt().toString() : null
        );
    }
}
