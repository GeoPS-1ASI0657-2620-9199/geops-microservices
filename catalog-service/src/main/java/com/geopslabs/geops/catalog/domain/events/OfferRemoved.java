package com.geopslabs.geops.catalog.domain.events;

import com.geopslabs.geops.catalog.domain.models.ModerationAction;
import com.geopslabs.geops.catalog.domain.models.Offer;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An administrator removed an offer of a campaign (US24). Like OfferExpired it lets Notification cancel the notices
 * of the offer and Engagement mark it unavailable among the saved ones. The reason stays in Catalog.
 */
public record OfferRemoved(UUID eventId, Instant occurredAt, Payload payload) implements DomainEvent {
    public static final String EVENT_TYPE = "OfferRemoved";
    public static final int VERSION = 1;

    public OfferRemoved {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(payload);
    }

    public static OfferRemoved of(Offer offer, ModerationAction action, UUID eventId) {
        if (offer.getCampaignId() == null) {
            throw new IllegalArgumentException("Only an offer of a campaign produces OfferRemoved");
        }
        return new OfferRemoved(eventId, action.removedAt(), new Payload(offer.getId(), offer.getCampaignId(),
                offer.getBusinessId(), action.removedAt()));
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

    public record Payload(Long offerId, Long campaignId, Long businessId, Instant removedAt) {
    }
}
