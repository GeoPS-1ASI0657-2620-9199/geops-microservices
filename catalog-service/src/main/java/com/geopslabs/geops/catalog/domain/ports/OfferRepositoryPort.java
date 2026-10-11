package com.geopslabs.geops.catalog.domain.ports;

import com.geopslabs.geops.catalog.domain.models.GeoCell;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.NearbyOfferCandidate;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.PublicSourceOffer;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface OfferRepositoryPort {
    Offer save(Offer offer);

    Optional<Offer> findById(Long id);

    List<Offer> findByCampaignId(Long campaignId);

    List<NearbyOfferCandidate> findPublishedWithin(GeoPoint origin, int radiusMeters, LocalDate today);

    /**
     * Every published offer of an active campaign, valid today, whose location falls in one of the cells, grouped by
     * the geohash of its cell. A cell without offers is absent from the map.
     */
    Map<String, List<NearbyOfferCandidate>> findPublishedInCells(Set<GeoCell> cells, LocalDate today);

    /** Public source offers with coordinates, published and valid today, within the radius. */
    List<PublicSourceOffer> findPublicSourceWithin(GeoPoint origin, int radiusMeters, LocalDate today);

    /** Locations of the published offers of a business that are still valid today. */
    List<GeoPoint> findPublishedLocationsOfBusiness(Long businessId, LocalDate today);

    /**
     * Marks EXPIRED the published offers whose validity ended before today and returns the locations of the ones
     * that had coordinates. Runs inside the caller's transaction.
     */
    List<GeoPoint> expirePublishedBefore(LocalDate today);

    /** Offers waiting for coordinates, in id order. */
    List<Offer> findPendingGeocoding();

    /** Sets the location and GEOCODED only if the offer is still PENDING; true when it changed. */
    boolean markGeocoded(Long offerId, GeoPoint location);

    /** Sets MANUAL_REVIEW only if the offer is still PENDING; true when it changed. */
    boolean markForManualReview(Long offerId);

    Map<GeocodingStatus, Long> countPublicSourceByGeocodingStatus();
}
