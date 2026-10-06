package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.commands.CreateOfferCommand;
import com.geopslabs.geops.catalog.domain.models.commands.UpdateOfferCommand;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

public class Offer {
    private Long id;
    private Campaign campaign;
    private String title;
    private String partner;
    private BigDecimal price;
    private String codePrefix;
    private LocalDate validTo;
    private Integer rating;
    private String location;
    private String category;
    private String imageUrl;
    private Date createdAt;
    private Date updatedAt;

    public Offer(Campaign campaign, CreateOfferCommand command) {
        this.campaign = campaign;
        this.title = command.title();
        this.partner = command.partner();
        this.price = command.price();
        this.codePrefix = command.codePrefix();
        this.validTo = command.validTo();
        this.rating = command.rating();
        this.location = command.location();
        this.category = command.category();
        this.imageUrl = command.imageUrl();
    }

    @SuppressWarnings("java:S107")
    public Offer(Long id, Campaign campaign, String title, String partner, BigDecimal price, String codePrefix,
                 LocalDate validTo, Integer rating, String location, String category, String imageUrl,
                 Date createdAt, Date updatedAt) {
        this.id = id;
        this.campaign = campaign;
        this.title = title;
        this.partner = partner;
        this.price = price;
        this.codePrefix = codePrefix;
        this.validTo = validTo;
        this.rating = rating;
        this.location = location;
        this.category = category;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateOffer(UpdateOfferCommand command) {
        if (command.title() != null) {
            this.title = command.title();
        }
        if (command.partner() != null) {
            this.partner = command.partner();
        }
        if (command.price() != null) {
            this.price = command.price();
        }
        if (command.codePrefix() != null) {
            this.codePrefix = command.codePrefix();
        }
        if (command.validTo() != null) {
            this.validTo = command.validTo();
        }
        if (command.rating() != null) {
            this.rating = command.rating();
        }
        if (command.location() != null) {
            this.location = command.location();
        }
        if (command.category() != null) {
            this.category = command.category();
        }
        if (command.imageUrl() != null) {
            this.imageUrl = command.imageUrl();
        }
    }

    public boolean isExpired() {
        return validTo != null && validTo.isBefore(LocalDate.now());
    }

    public Long getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public String getTitle() {
        return title;
    }

    public String getPartner() {
        return partner;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCodePrefix() {
        return codePrefix;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public Integer getRating() {
        return rating;
    }

    public String getLocation() {
        return location;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}
