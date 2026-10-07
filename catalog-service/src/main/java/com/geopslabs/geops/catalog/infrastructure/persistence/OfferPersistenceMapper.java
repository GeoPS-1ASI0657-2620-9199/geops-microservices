package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.Offer;

final class OfferPersistenceMapper {

    private OfferPersistenceMapper() {
    }

    static Offer toDomain(OfferJpaEntity entity) {
        return new Offer(entity.getId(), entity.getCampaignId(), entity.getBusinessId(), entity.getTitle(),
                entity.getConditions(), Money.soles(entity.getPrice()), entity.getValidTo(), entity.getCategory(),
                entity.getGeocodingStatus(), entity.getAddress(), entity.getImageUrl(), entity.getSource(),
                entity.getSourceName(), entity.getStatus(), GeographyPoints.toGeoPoint(entity.getLocation()));
    }

    static OfferJpaEntity toEntity(Offer offer) {
        var entity = new OfferJpaEntity();
        entity.setId(offer.getId());
        entity.setCampaignId(offer.getCampaignId());
        entity.setBusinessId(offer.getBusinessId());
        entity.setTitle(offer.getTitle());
        entity.setConditions(offer.getConditions());
        entity.setPrice(offer.getPrice().amount());
        entity.setValidTo(offer.getValidTo());
        entity.setCategory(offer.getCategory());
        entity.setGeocodingStatus(offer.getGeocodingStatus());
        entity.setAddress(offer.getAddress());
        entity.setImageUrl(offer.getImageUrl());
        entity.setSource(offer.getSource());
        entity.setSourceName(offer.getSourceName());
        entity.setStatus(offer.getStatus());
        entity.setLocation(GeographyPoints.toPoint(offer.getLocation()));
        return entity;
    }
}
