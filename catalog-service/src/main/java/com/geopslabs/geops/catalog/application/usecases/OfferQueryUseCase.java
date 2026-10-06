package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.queries.GetAllOffersByCampaignIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetAllOffersQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetOfferByIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetOffersByIdsQuery;

import java.util.List;
import java.util.Optional;

public interface OfferQueryUseCase {

    List<Offer> handle(GetAllOffersQuery query);

    Optional<Offer> handle(GetOfferByIdQuery query);

    List<Offer> handle(GetOffersByIdsQuery query);

    List<Offer> handle(GetAllOffersByCampaignIdQuery query);
}
