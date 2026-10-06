package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.commands.CreateOfferCommand;
import com.geopslabs.geops.catalog.domain.models.commands.UpdateOfferCommand;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Offer {
    private Long id;
    private Campaign campaign;
    private String title;
    private Long businessId;
    private BigDecimal price;
    private LocalDate validTo;
    private String location;
    private String category;
    private String imageUrl;
    public Offer(Campaign campaign, CreateOfferCommand command) {
        this.campaign = campaign;
        this.title = command.title();
        this.businessId = command.businessId();
        this.price = command.price();
        this.validTo = command.validTo();
        this.location = command.location();
        this.category = command.category();
        this.imageUrl = command.imageUrl();
    }

    @SuppressWarnings("java:S107")
    public Offer(Long id, Campaign campaign, String title, Long businessId, BigDecimal price,
                 LocalDate validTo, String location, String category, String imageUrl) {
        this.id = id;
        this.campaign = campaign;
        this.title = title;
        this.businessId = businessId;
        this.price = price;
        this.validTo = validTo;
        this.location = location;
        this.category = category;
        this.imageUrl = imageUrl;
    }

    public void updateOffer(UpdateOfferCommand command) {
        if (command.title() != null) {
            this.title = command.title();
        }
        if (command.businessId() != null) {
            this.businessId = command.businessId();
        }
        if (command.price() != null) {
            this.price = command.price();
        }
        if (command.validTo() != null) {
            this.validTo = command.validTo();
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

    public Long getBusinessId() {
        return businessId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public LocalDate getValidTo() {
        return validTo;
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
}
