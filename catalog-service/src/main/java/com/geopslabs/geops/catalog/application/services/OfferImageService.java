package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.UploadOfferImageUseCase;
import com.geopslabs.geops.catalog.domain.models.OfferImage;
import com.geopslabs.geops.catalog.domain.ports.OfferImageRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Transactional
public class OfferImageService implements UploadOfferImageUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(OfferImageService.class);

    private final OfferImageRepositoryPort images;
    private final Clock clock;

    public OfferImageService(OfferImageRepositoryPort images, Clock clock) {
        this.images = images;
        this.clock = clock;
    }

    @Override
    public OfferImage upload(Long businessId, byte[] content) {
        var image = images.save(OfferImage.upload(businessId, content, clock.instant()));
        LOGGER.info("offer.image.uploaded imageId={} businessId={} contentType={} bytes={}",
                image.imageId(), businessId, image.contentType(), image.content().length);
        return image;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OfferImage> find(UUID imageId) {
        return images.findById(imageId);
    }
}
