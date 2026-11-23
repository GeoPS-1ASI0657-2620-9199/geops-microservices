package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.OfferImage;
import com.geopslabs.geops.catalog.domain.ports.OfferImageRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** The photos live in catalog_db, next to the offers that use them: no extra volume nor service to deploy. */
@Component
public class OfferImageJpaAdapter implements OfferImageRepositoryPort {
    private final OfferImageJpaRepository repository;

    public OfferImageJpaAdapter(OfferImageJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public OfferImage save(OfferImage image) {
        var entity = new OfferImageJpaEntity();
        entity.setImageId(image.imageId());
        entity.setBusinessId(image.businessId());
        entity.setContentType(image.contentType());
        entity.setContent(image.content());
        entity.setUploadedAt(image.uploadedAt());
        repository.save(entity);
        return image;
    }

    @Override
    public Optional<OfferImage> findById(UUID imageId) {
        return repository.findById(imageId).map(entity -> new OfferImage(entity.getImageId(), entity.getBusinessId(),
                entity.getContentType(), entity.getContent(), entity.getUploadedAt()));
    }
}
