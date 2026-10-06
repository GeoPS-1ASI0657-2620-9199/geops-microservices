package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.Offer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

final class OfferPersistenceMapper {
    private static final int WGS84 = 4326;
    private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), WGS84);

    private OfferPersistenceMapper() {
    }

    static Offer toDomain(OfferJpaEntity entity) {
        return new Offer(entity.getId(), entity.getCampaignId(), entity.getBusinessId(), entity.getTitle(),
                entity.getConditions(), Money.soles(entity.getPrice()), entity.getValidTo(), entity.getCategory(),
                entity.getGeocodingStatus(), entity.getAddress(), entity.getImageUrl(), entity.getSource(),
                entity.getSourceName(), entity.getStatus(), toGeoPoint(entity.getLocation()));
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
        entity.setLocation(toPoint(offer.getLocation()));
        return entity;
    }

    private static GeoPoint toGeoPoint(Point point) {
        return point == null ? null : new GeoPoint(point.getY(), point.getX());
    }

    private static Point toPoint(GeoPoint location) {
        return location == null ? null
                : GEOMETRY.createPoint(new Coordinate(location.longitude(), location.latitude()));
    }
}
