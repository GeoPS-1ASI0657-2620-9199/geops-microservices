package com.geopslabs.geops.catalog.domain.events;

import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.Offer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record OfferPublished(UUID eventId, Instant occurredAt, Payload payload) implements DomainEvent {
    public static final String EVENT_TYPE = "OfferPublished";
    public static final int VERSION = 1;

    public OfferPublished {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(payload);
    }

    public static OfferPublished of(Campaign campaign, Offer offer, String businessName, boolean reactivated,
                                    UUID eventId, Instant now) {
        return new OfferPublished(eventId, now, new Payload(offer.getId(), campaign.getId(),
                campaign.getBusinessId(), businessName, offer.getTitle(), offer.getCategory(),
                offer.getPrice().amount(), offer.getValidTo(), Point.of(offer.getLocation()),
                Zone.of(campaign.getZone()), reactivated, now));
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public int version() {
        return VERSION;
    }

    @Override
    public Long aggregateId() {
        return payload.campaignId();
    }

    public record Payload(Long offerId, Long campaignId, Long businessId, String businessName, String title,
                          String category, BigDecimal price, LocalDate validTo, Point location, Zone zone,
                          boolean reactivated, Instant publishedAt) {
    }

    public record Point(Double latitude, Double longitude) {
        static Point of(GeoPoint point) {
            return point == null ? null : new Point(point.latitude(), point.longitude());
        }
    }

    public record Zone(String type, Point center, Integer radiusMeters, String district, Integer reachMeters) {
        static Zone of(CampaignZone zone) {
            if (zone == null) {
                return null;
            }
            if (zone.reachesEverywhere()) {
                return new Zone(zone.type().name(), null, zone.radiusMeters(), zone.district(), null);
            }
            return new Zone(zone.type().name(), Point.of(zone.center()), zone.radiusMeters(), zone.district(),
                    zone.reachMeters());
        }
    }
}
