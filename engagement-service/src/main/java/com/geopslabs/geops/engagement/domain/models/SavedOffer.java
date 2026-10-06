package com.geopslabs.geops.engagement.domain.models;

import java.time.LocalDateTime;

public class SavedOffer {
    private final Long id;
    private final Long consumerId;
    private final Long offerId;
    private final LocalDateTime savedAt;

    public SavedOffer(Long id, Long consumerId, Long offerId, LocalDateTime savedAt) {
        this.id = id;
        this.consumerId = consumerId;
        this.offerId = offerId;
        this.savedAt = savedAt;
    }

    public static SavedOffer create(Long consumerId, Long offerId, LocalDateTime savedAt) {
        return new SavedOffer(null, consumerId, offerId, savedAt);
    }

    public Long getId() {
        return id;
    }

    public Long getConsumerId() {
        return consumerId;
    }

    public Long getOfferId() {
        return offerId;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }
}
