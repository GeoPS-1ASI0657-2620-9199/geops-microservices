package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.queries.GetOffersByCampaignIdQuery;

import java.util.List;

public interface ListCampaignOffersUseCase {
    List<Offer> list(GetOffersByCampaignIdQuery query);
}
