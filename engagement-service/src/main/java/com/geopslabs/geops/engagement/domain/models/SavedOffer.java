package com.geopslabs.geops.engagement.domain.models;

import com.geopslabs.geops.engagement.domain.models.commands.SaveOfferCommand;
import com.geopslabs.geops.backend.identity.domain.model.aggregates.User;
import com.geopslabs.geops.backend.offers.domain.model.aggregates.Offer;

import java.util.Date;

public class SavedOffer {
    private Long id;
    private User user;
    private Offer offer;
    private Date createdAt;
    private Date updatedAt;

    public SavedOffer(SaveOfferCommand command, User user, Offer offer) {
        this.user = user;
        this.offer = offer;
    }

    public SavedOffer(Long id, User user, Offer offer, Date createdAt, Date updatedAt) {
        this.id = id;
        this.user = user;
        this.offer = offer;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Offer getOffer() {
        return offer;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public Long getUserId() {
        return this.user != null ? this.user.getId() : null;
    }

    public Long getOfferId() {
        return this.offer != null ? this.offer.getId() : null;
    }

    public boolean belongsToUser(Long userId) {
        return this.user != null && this.user.getId().equals(userId);
    }

    public boolean isForOffer(Long offerId) {
        return this.offer != null && this.offer.getId().equals(offerId);
    }
}
