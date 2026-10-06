package com.geopslabs.geops.engagement.domain.models;

import com.geopslabs.geops.engagement.domain.models.commands.SaveOfferCommand;

import java.time.LocalDateTime;

public class SavedOffer {
    private Long id;
    private Long consumerId;
    private Long offerId;
    private LocalDateTime savedAt;

    public SavedOffer(SaveOfferCommand command) {
        this.consumerId = command.consumerId();
        this.offerId = command.offerId();
    }

    public SavedOffer(Long id, Long consumerId, Long offerId, LocalDateTime savedAt) {
        this.id = id;
        this.consumerId = consumerId;
        this.offerId = offerId;
        this.savedAt = savedAt;
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

    public boolean belongsToConsumer(Long candidateConsumerId) {
        return consumerId.equals(candidateConsumerId);
    }

    public boolean isForOffer(Long candidateOfferId) {
        return offerId.equals(candidateOfferId);
    }
}
