package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.OfferImage;

import java.util.Optional;
import java.util.UUID;

/** Photos of the offers: the business uploads one before publishing and anyone reads it from the card. */
public interface UploadOfferImageUseCase {
    OfferImage upload(Long businessId, byte[] content);

    Optional<OfferImage> find(UUID imageId);
}
