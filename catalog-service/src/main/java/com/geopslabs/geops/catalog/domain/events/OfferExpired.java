package com.geopslabs.geops.catalog.domain.events;

import com.geopslabs.geops.catalog.domain.models.ExpiredOffer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record OfferExpired(UUID eventId, Instant occurredAt, Payload payload) implements DomainEvent {
    public static final String EVENT_TYPE = "OfferExpired";
    public static final int VERSION = 1;

    public OfferExpired {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(payload);
    }

    public static OfferExpired of(ExpiredOffer offer, UUID eventId, Instant now) {
        if (!offer.belongsToCampaign()) {
            throw new IllegalArgumentException("Only an offer of a campaign produces OfferExpired");
        }
        return new OfferExpired(eventId, now, new Payload(offer.offerId(), offer.campaignId(), offer.businessId(),
                offer.validTo(), now));
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

    public record Payload(Long offerId, Long campaignId, Long businessId, LocalDate validTo, Instant expiredAt) {
    }
}
