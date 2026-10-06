package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.Offer;

final class OfferPersistenceMapper {

    private OfferPersistenceMapper() {
    }

    static Offer toDomain(OfferJpaEntity entity) {
        return new Offer(entity.getId(), CampaignPersistenceMapper.toDomain(entity.getCampaign()), entity.getTitle(),
                entity.getPartner(), entity.getPrice(), entity.getCodePrefix(), entity.getValidTo(),
                entity.getRating(), entity.getLocation(), entity.getCategory(), entity.getImageUrl(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    static OfferJpaEntity toEntity(Offer offer) {
        var entity = new OfferJpaEntity();
        entity.setId(offer.getId());
        entity.setCampaign(CampaignPersistenceMapper.toEntity(offer.getCampaign()));
        entity.setTitle(offer.getTitle());
        entity.setPartner(offer.getPartner());
        entity.setPrice(offer.getPrice());
        entity.setCodePrefix(offer.getCodePrefix());
        entity.setValidTo(offer.getValidTo());
        entity.setRating(offer.getRating());
        entity.setLocation(offer.getLocation());
        entity.setCategory(offer.getCategory());
        entity.setImageUrl(offer.getImageUrl());
        return entity;
    }
}
