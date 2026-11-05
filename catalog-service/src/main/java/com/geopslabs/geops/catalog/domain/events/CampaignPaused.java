package com.geopslabs.geops.catalog.domain.events;

import com.geopslabs.geops.catalog.domain.models.Campaign;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CampaignPaused(UUID eventId, Instant occurredAt, Payload payload) implements DomainEvent {
    public static final String EVENT_TYPE = "CampaignPaused";
    public static final int VERSION = 1;

    public CampaignPaused {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(payload);
    }

    public static CampaignPaused of(Campaign campaign, List<Long> offerIds, UUID eventId, Instant now) {
        return new CampaignPaused(eventId, now,
                new Payload(campaign.getId(), campaign.getBusinessId(), List.copyOf(offerIds), now));
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

    public record Payload(Long campaignId, Long businessId, List<Long> offerIds, Instant pausedAt) {
    }
}
