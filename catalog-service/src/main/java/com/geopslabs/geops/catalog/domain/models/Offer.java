package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidRegularPriceException;
import com.geopslabs.geops.catalog.domain.models.exceptions.OfferAlreadyRemovedException;
import com.geopslabs.geops.catalog.domain.models.exceptions.OfferValidityOutsideCampaignException;

import java.time.Instant;
import java.time.LocalDate;

public class Offer {
    private static final Money NO_REGULAR_PRICE = null;

    private final Long id;
    private final Long campaignId;
    private final Long businessId;
    private final String title;
    private final String conditions;
    private final Money price;
    private final Money regularPrice;
    private final LocalDate validTo;
    private final String category;
    private final GeocodingStatus geocodingStatus;
    private final String address;
    private final String imageUrl;
    private final OfferSource source;
    private final String sourceName;
    private final OfferStatus status;
    private final GeoPoint location;

    /** An offer without a regular price. */
    @SuppressWarnings("java:S107")
    public Offer(Long id, Long campaignId, Long businessId, String title, String conditions, Money price,
                 LocalDate validTo, String category, GeocodingStatus geocodingStatus, String address,
                 String imageUrl, OfferSource source, String sourceName, OfferStatus status,
                 GeoPoint location) {
        this(id, campaignId, businessId, title, conditions, price, NO_REGULAR_PRICE, validTo, category,
                geocodingStatus, address, imageUrl, source, sourceName, status, location);
    }

    @SuppressWarnings("java:S107")
    public Offer(Long id, Long campaignId, Long businessId, String title, String conditions, Money price,
                 Money regularPrice, LocalDate validTo, String category, GeocodingStatus geocodingStatus,
                 String address, String imageUrl, OfferSource source, String sourceName, OfferStatus status,
                 GeoPoint location) {
        this.id = id;
        this.campaignId = campaignId;
        this.businessId = businessId;
        this.title = title;
        this.conditions = conditions;
        this.price = price;
        this.regularPrice = regularPrice;
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

    /**
     * The regular price is optional; when the merchant gives it, it must be above the offer price so the saving the
     * consumer sees is real.
     */
    @SuppressWarnings("java:S107")
    public static Offer publishFor(Campaign campaign, String title, String conditions, Money price,
                                   Money regularPrice, LocalDate validTo, String category, String imageUrl,
                                   String address, GeoPoint location) {
        if (!campaign.getPeriod().contains(validTo)) {
            throw new OfferValidityOutsideCampaignException(title);
        }
        if (regularPrice != null && regularPrice.amount().compareTo(price.amount()) <= 0) {
            throw new InvalidRegularPriceException(title);
        }
        return new Offer(null, campaign.getId(), campaign.getBusinessId(), title, conditions, price, regularPrice,
                validTo, category, GeocodingStatus.GEOCODED, address, imageUrl, OfferSource.AFFILIATED, null,
                OfferStatus.PUBLISHED, location);
    }

    /**
     * An offer loaded by the administrator from a public source (E2-09): no campaign, no business, and no coordinates
     * until the geocoding run finds its address.
     */
    @SuppressWarnings("java:S107")
    public static Offer fromPublicSource(String title, String conditions, Money price, LocalDate validTo,
                                         String category, String address, String sourceName, String imageUrl) {
        return new Offer(null, null, null, title, conditions, price, validTo, category, GeocodingStatus.PENDING,
                address, imageUrl, OfferSource.PUBLIC_SOURCE, sourceName, OfferStatus.PUBLISHED, null);
    }

    public Offer inCampaign(Long savedCampaignId) {
        return new Offer(id, savedCampaignId, businessId, title, conditions, price, regularPrice, validTo, category,
                geocodingStatus, address, imageUrl, source, sourceName, status, location);
    }

    /**
     * US24: the administrator takes the offer out of the catalog. The offer is kept with status REMOVED, so the
     * reservations that point to it keep their reference; the action records who removed it, when and why.
     */
    public ModerationAction remove(Long adminId, String reason, Instant removedAt) {
        if (status == OfferStatus.REMOVED) {
            throw new OfferAlreadyRemovedException(id);
        }
        return new ModerationAction(null, id, adminId, reason, removedAt);
    }

    public Offer asRemoved() {
        return new Offer(id, campaignId, businessId, title, conditions, price, validTo, category, geocodingStatus,
                address, imageUrl, source, sourceName, OfferStatus.REMOVED, location);
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

    /** Null when the merchant did not give a regular price. */
    public Money getRegularPrice() {
        return regularPrice;
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
