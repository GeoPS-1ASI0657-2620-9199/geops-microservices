package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.OfferValidityOutsideCampaignException;

import java.time.LocalDate;

public class Offer {
    private final Long id;
    private final Long campaignId;
    private final Long businessId;
    private final String title;
    private final String conditions;
    private final Money price;
    private final LocalDate validTo;
    private final String category;
    private final GeocodingStatus geocodingStatus;
    private final String address;
    private final String imageUrl;
    private final OfferSource source;
    private final String sourceName;
    private final OfferStatus status;
    private final GeoPoint location;

    @SuppressWarnings("java:S107")
    public Offer(Long id, Long campaignId, Long businessId, String title, String conditions, Money price,
                 LocalDate validTo, String category, GeocodingStatus geocodingStatus, String address,
                 String imageUrl, OfferSource source, String sourceName, OfferStatus status,
                 GeoPoint location) {
        this.id = id;
        this.campaignId = campaignId;
        this.businessId = businessId;
        this.title = title;
        this.conditions = conditions;
        this.price = price;
        this.validTo = validTo;
        this.category = category;
        this.geocodingStatus = geocodingStatus;
        this.address = address;
        this.imageUrl = imageUrl;
        this.source = source;
        this.sourceName = sourceName;
        this.status = status;
        this.location = location;
    }

    @SuppressWarnings("java:S107")
    public static Offer publishFor(Campaign campaign, String title, String conditions, Money price, LocalDate validTo,
                                   String category, String imageUrl, String address, GeoPoint location) {
        if (!campaign.getPeriod().contains(validTo)) {
            throw new OfferValidityOutsideCampaignException(title);
        }
        return new Offer(null, campaign.getId(), campaign.getBusinessId(), title, conditions, price, validTo,
                category, GeocodingStatus.GEOCODED, address, imageUrl, OfferSource.AFFILIATED, null,
                OfferStatus.PUBLISHED, location);
    }

    public Offer inCampaign(Long savedCampaignId) {
        return new Offer(id, savedCampaignId, businessId, title, conditions, price, validTo, category,
                geocodingStatus, address, imageUrl, source, sourceName, status, location);
    }

    public boolean isValidOn(LocalDate date) {
        return status == OfferStatus.PUBLISHED && !validTo.isBefore(date);
    }

    public boolean isReservableOn(LocalDate date) {
        return source == OfferSource.AFFILIATED && isValidOn(date);
    }

    public Long getId() {
        return id;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public String getTitle() {
        return title;
    }

    public String getConditions() {
        return conditions;
    }

    public Money getPrice() {
        return price;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public String getCategory() {
        return category;
    }

    public GeocodingStatus getGeocodingStatus() {
        return geocodingStatus;
    }

    public String getAddress() {
        return address;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public OfferSource getSource() {
        return source;
    }

    public String getSourceName() {
        return sourceName;
    }

    public OfferStatus getStatus() {
        return status;
    }

    public GeoPoint getLocation() {
        return location;
    }
}
