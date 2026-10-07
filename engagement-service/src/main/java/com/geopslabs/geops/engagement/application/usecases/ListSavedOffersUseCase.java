package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOffersByConsumerQuery;

import java.util.List;

public interface ListSavedOffersUseCase {
    List<SavedOfferView> list(GetSavedOffersByConsumerQuery query);
}
