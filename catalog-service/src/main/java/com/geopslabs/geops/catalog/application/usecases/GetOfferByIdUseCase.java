package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.queries.GetOfferByIdQuery;

public interface GetOfferByIdUseCase {
    Offer getById(GetOfferByIdQuery query);
}
