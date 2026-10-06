package com.geopslabs.geops.engagement.domain.models;

import com.geopslabs.geops.backend.identity.domain.model.aggregates.User;
import com.geopslabs.geops.backend.offers.domain.model.aggregates.Offer;
import com.geopslabs.geops.engagement.domain.models.commands.CreateReviewCommand;
import com.geopslabs.geops.engagement.domain.models.commands.UpdateReviewCommand;

import java.util.Date;

public class Review {
    private Long id;
    private Offer offer;
    private User user;
    private String userName;
    private Integer rating;
    private String text;
    private Integer likes;
    private Date createdAt;
    private Date updatedAt;

    public Review(CreateReviewCommand command, User user, Offer offer) {
        this.offer = offer;
        this.user = user;
        this.userName = command.userName();
        this.rating = command.rating();
        this.text = command.text();
        this.likes = 0;
    }

    public Review(Long id, Offer offer, User user, String userName, Integer rating, String text, Integer likes,
                  Date createdAt, Date updatedAt) {
        this.id = id;
        this.offer = offer;
        this.user = user;
        this.userName = userName;
        this.rating = rating;
        this.text = text;
        this.likes = likes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Offer getOffer() {
        return offer;
    }

    public User getUser() {
        return user;
    }

    public String getUserName() {
        return userName;
    }

    public Integer getRating() {
        return rating;
    }

    public String getText() {
        return text;
    }

    public Integer getLikes() {
        return likes;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public Long getOfferId() {
        return this.offer != null ? this.offer.getId() : null;
    }

    public Long getUserId() {
        return this.user != null ? this.user.getId() : null;
    }

    public void updateReview(UpdateReviewCommand command) {
        if (command.text() != null) {
            this.text = command.text();
        }
        if (command.likes() != null) {
            this.likes = command.likes();
        }
    }

    public void incrementLikes() {
        this.likes++;
    }

    public void decrementLikes() {
        if (this.likes > 0) {
            this.likes--;
        }
    }

    public boolean hasValidRating() {
        return this.rating != null && this.rating >= 1 && this.rating <= 5;
    }
}
