package com.geopslabs.geops.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OfferJpaRepository extends JpaRepository<OfferJpaEntity, Long> {
    String NEARBY_QUERY = """
            SELECT o.id                       AS "offerId",
                   o.title                    AS "title",
                   o.price                    AS "price",
                   o.valid_to                 AS "validTo",
                   o.category                 AS "category",
                   ST_Y(o.location::geometry) AS "latitude",
                   ST_X(o.location::geometry) AS "longitude",
                   c.business_id              AS "businessId",
                   m.business_name            AS "businessName",
                   m.verified_seal            AS "verifiedSeal",
                   m.open_reports             AS "openReports"
            FROM offers o
            JOIN campaigns c ON c.id = o.campaign_id
            JOIN merchant_standings m ON m.business_id = c.business_id
            WHERE o.status = :offerStatus
              AND o.valid_to >= :today
              AND c.status = :campaignStatus
              AND c.end_date >= :today
              AND ST_DWithin(o.location,
                             ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                             :radiusMeters,
                             false)
            """;

    List<OfferJpaEntity> findByCampaignIdOrderByIdAsc(Long campaignId);

    @Query(value = NEARBY_QUERY, nativeQuery = true)
    List<NearbyOfferRow> findPublishedWithin(@Param("latitude") double latitude,
                                             @Param("longitude") double longitude,
                                             @Param("radiusMeters") int radiusMeters,
                                             @Param("today") LocalDate today,
                                             @Param("offerStatus") String offerStatus,
                                             @Param("campaignStatus") String campaignStatus);
}
