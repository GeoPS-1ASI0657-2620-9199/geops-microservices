package com.geopslabs.geops.catalog.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Offer {
    private final Long id;
    private final Campaign campaign;
    private final String title;
    private final Long businessId;
    private final BigDecimal price;
    private final LocalDate validTo;
    private final String location;
    private final String category;
    private final String imageUrl;

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
