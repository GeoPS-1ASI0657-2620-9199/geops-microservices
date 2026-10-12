package com.geopslabs.geops.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface OfferJpaRepository extends JpaRepository<OfferJpaEntity, Long> {
    String CANDIDATE_COLUMNS = """
            SELECT o.id                       AS "offerId",
                   o.title                    AS "title",
                   o.price                    AS "price",
                   o.valid_to                 AS "validTo",
                   o.category                 AS "category",
                   o.address                  AS "address",
                   o.image_url                AS "imageUrl",
                   ST_Y(o.location::geometry) AS "latitude",
                   ST_X(o.location::geometry) AS "longitude",
                   c.business_id              AS "businessId",
                   m.business_name            AS "businessName",
                   m.verified_seal            AS "verifiedSeal",
                   m.open_reports             AS "openReports",
                   c.zone_type                AS "zoneType",
                   ST_Y(c.zone_center::geometry) AS "zoneLatitude",
                   ST_X(c.zone_center::geometry) AS "zoneLongitude",
                   c.zone_radius_m            AS "zoneRadiusMeters",
                   c.zone_district            AS "zoneDistrict"
            """;
    String PUBLISHED_IN_ACTIVE_CAMPAIGN = """
            FROM offers o
            JOIN campaigns c ON c.id = o.campaign_id
            JOIN merchant_standings m ON m.business_id = c.business_id
            WHERE o.status = :offerStatus
              AND o.valid_to >= :today
              AND c.status = :campaignStatus
              AND c.end_date >= :today
            """;
    String NEARBY_QUERY = CANDIDATE_COLUMNS + PUBLISHED_IN_ACTIVE_CAMPAIGN + """
              AND ST_DWithin(o.location,
                             ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                             :radiusMeters,
                             false)
            """;
    /**
     * Consulta por celda (Informe 4.3.2.5): ST_DWithin over the circle that wraps the cells uses the GiST index on
     * location, and ST_GeoHash keeps exactly the offers of the cells asked for.
     */
    String CELLS_QUERY = CANDIDATE_COLUMNS + """
                 , ST_GeoHash(o.location::geometry, 6) AS "cell"
            """ + PUBLISHED_IN_ACTIVE_CAMPAIGN + """
              AND ST_DWithin(o.location,
                             ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                             :radiusMeters,
                             false)
              AND ST_GeoHash(o.location::geometry, 6) IN (:cells)
            """;
    /** Public source offers have no campaign, so this query does not join campaigns nor merchant_standings. */
    String PUBLIC_NEARBY_QUERY = """
            SELECT o.id                       AS "offerId",
                   o.title                    AS "title",
                   o.source_name              AS "sourceName",
                   o.price                    AS "price",
                   o.valid_to                 AS "validTo",
                   o.category                 AS "category",
                   o.address                  AS "address",
                   o.image_url                AS "imageUrl",
                   ST_Y(o.location::geometry) AS "latitude",
                   ST_X(o.location::geometry) AS "longitude"
            FROM offers o
            WHERE o.source = :source
              AND o.geocoding_status = :geocodingStatus
              AND o.status = :offerStatus
              AND o.valid_to >= :today
              AND ST_DWithin(o.location,
                             ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                             :radiusMeters,
                             false)
            """;
    String BUSINESS_LOCATIONS_QUERY = """
            SELECT DISTINCT ST_Y(o.location::geometry) AS "latitude",
                            ST_X(o.location::geometry) AS "longitude"
            FROM offers o
            WHERE o.business_id = :businessId
              AND o.status = :offerStatus
              AND o.valid_to >= :today
              AND o.location IS NOT NULL
            """;
    String EXPIRE_QUERY = """
            WITH expired AS (
                UPDATE offers SET status = :expiredStatus
                WHERE status = :offerStatus AND valid_to < :today
                RETURNING location
            )
            SELECT ST_Y(location::geometry) AS "latitude", ST_X(location::geometry) AS "longitude"
            FROM expired
            WHERE location IS NOT NULL
            """;

    List<OfferJpaEntity> findByCampaignIdOrderByIdAsc(Long campaignId);

    @Query(value = NEARBY_QUERY, nativeQuery = true)
    List<NearbyOfferRow> findPublishedWithin(@Param("latitude") double latitude,
                                             @Param("longitude") double longitude,
                                             @Param("radiusMeters") int radiusMeters,
                                             @Param("today") LocalDate today,
                                             @Param("offerStatus") String offerStatus,
                                             @Param("campaignStatus") String campaignStatus);

    @SuppressWarnings("java:S107")
    @Query(value = CELLS_QUERY, nativeQuery = true)
    List<CellOfferRow> findPublishedInCells(@Param("latitude") double latitude,
                                            @Param("longitude") double longitude,
                                            @Param("radiusMeters") double radiusMeters,
                                            @Param("cells") Collection<String> cells,
                                            @Param("today") LocalDate today,
                                            @Param("offerStatus") String offerStatus,
                                            @Param("campaignStatus") String campaignStatus);

    @SuppressWarnings("java:S107")
    @Query(value = PUBLIC_NEARBY_QUERY, nativeQuery = true)
    List<PublicOfferRow> findPublicSourceWithin(@Param("latitude") double latitude,
                                                @Param("longitude") double longitude,
                                                @Param("radiusMeters") int radiusMeters,
                                                @Param("today") LocalDate today,
                                                @Param("source") String source,
                                                @Param("geocodingStatus") String geocodingStatus,
                                                @Param("offerStatus") String offerStatus);

    @Query(value = BUSINESS_LOCATIONS_QUERY, nativeQuery = true)
    List<LocationRow> findLocationsOfBusiness(@Param("businessId") Long businessId,
                                              @Param("today") LocalDate today,
                                              @Param("offerStatus") String offerStatus);

    /** A data-modifying CTE: PostgreSQL runs the UPDATE and answers its rows like a query. */
    @Transactional
    @Query(value = EXPIRE_QUERY, nativeQuery = true)
    List<LocationRow> expirePublishedBefore(@Param("today") LocalDate today,
                                            @Param("offerStatus") String offerStatus,
                                            @Param("expiredStatus") String expiredStatus);

    List<OfferJpaEntity> findByGeocodingStatusOrderByIdAsc(com.geopslabs.geops.catalog.domain.models.GeocodingStatus status);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE offers
            SET geocoding_status = :geocoded,
                location = ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
            WHERE id = :offerId AND geocoding_status = :pending
            """, nativeQuery = true)
    int markGeocoded(@Param("offerId") Long offerId, @Param("latitude") double latitude,
                     @Param("longitude") double longitude, @Param("geocoded") String geocoded,
                     @Param("pending") String pending);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE offers SET geocoding_status = :manualReview
            WHERE id = :offerId AND geocoding_status = :pending
            """, nativeQuery = true)
    int markForManualReview(@Param("offerId") Long offerId, @Param("manualReview") String manualReview,
                            @Param("pending") String pending);

    @Query(value = """
            SELECT geocoding_status AS "status", count(*) AS "total"
            FROM offers
            WHERE source = :source
            GROUP BY geocoding_status
            """, nativeQuery = true)
    List<StatusCountRow> countByGeocodingStatus(@Param("source") String source);

    interface StatusCountRow {
        String getStatus();

        Long getTotal();
    }

    interface CellOfferRow extends NearbyOfferRow {
        String getCell();
    }

    interface PublicOfferRow extends LocationRow {
        Long getOfferId();

        String getTitle();

        String getSourceName();

        java.math.BigDecimal getPrice();

        LocalDate getValidTo();

        String getCategory();

        String getAddress();

        String getImageUrl();
    }

    interface LocationRow {
        Double getLatitude();

        Double getLongitude();
    }
}
