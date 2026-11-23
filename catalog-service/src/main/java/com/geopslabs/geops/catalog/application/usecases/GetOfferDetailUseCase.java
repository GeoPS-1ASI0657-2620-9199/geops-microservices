package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.queries.GetOfferDetailQuery;

public interface GetOfferDetailUseCase {
    OfferDetail getDetail(GetOfferDetailQuery query);
}
