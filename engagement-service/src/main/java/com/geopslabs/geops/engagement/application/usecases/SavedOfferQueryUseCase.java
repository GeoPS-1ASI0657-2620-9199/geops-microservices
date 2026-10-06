package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOfferByIdQuery;
import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOfferByConsumerIdAndOfferIdQuery;
import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOffersByConsumerQuery;

import java.util.List;
import java.util.Optional;

public interface SavedOfferQueryUseCase {
    List<SavedOffer> handle(GetSavedOffersByConsumerQuery query);

    Optional<SavedOffer> handle(GetSavedOfferByConsumerIdAndOfferIdQuery query);

    Optional<SavedOffer> handle(GetSavedOfferByIdQuery query);
}

