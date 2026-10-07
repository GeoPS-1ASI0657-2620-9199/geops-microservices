package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.queries.GetOfferByIdQuery;

public interface GetOfferDetailUseCase {
    OfferDetail getDetail(GetOfferByIdQuery query);
}
