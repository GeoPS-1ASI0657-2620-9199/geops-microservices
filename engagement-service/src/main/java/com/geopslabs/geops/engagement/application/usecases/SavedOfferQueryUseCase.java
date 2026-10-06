package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOffersByConsumerQuery;

import java.util.List;

public interface SavedOfferQueryUseCase {
    List<SavedOffer> handle(GetSavedOffersByConsumerQuery query);
}
