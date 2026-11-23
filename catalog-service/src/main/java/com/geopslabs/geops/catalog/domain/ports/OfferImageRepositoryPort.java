package com.geopslabs.geops.catalog.domain.ports;

import com.geopslabs.geops.catalog.domain.models.OfferImage;

import java.util.Optional;
import java.util.UUID;

public interface OfferImageRepositoryPort {
    OfferImage save(OfferImage image);

    Optional<OfferImage> findById(UUID imageId);
}
