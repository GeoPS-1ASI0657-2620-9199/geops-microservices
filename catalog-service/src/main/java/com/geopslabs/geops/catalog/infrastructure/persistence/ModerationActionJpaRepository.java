package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

/**
 * The listings of the moderation panel. No parameter is ever null: an absent filter arrives as every status, as
 * reportedOnly false and as the pattern that matches any text, so PostgreSQL never has to guess the type of a null.
 */
public interface ModerationActionJpaRepository extends JpaRepository<ModerationActionJpaEntity, Long> {
    String OFFERS_FILTER = """
            FROM OfferJpaEntity o
            LEFT JOIN MerchantStandingJpaEntity m ON m.businessId = o.businessId
            WHERE o.status IN :statuses
              AND (:reportedOnly = false OR m.openReports > 0)
              AND (LOWER(o.title) LIKE :pattern ESCAPE '!'
                   OR LOWER(COALESCE(m.businessName, o.sourceName, '')) LIKE :pattern ESCAPE '!')
            """;

    @Query(value = """
            SELECT new com.geopslabs.geops.catalog.infrastructure.persistence.ModerationOfferRow(
                   o.id, o.title, o.category, o.source, o.businessId, COALESCE(m.businessName, o.sourceName),
                   o.status, o.validTo, COALESCE(m.openReports, 0), COALESCE(m.verifiedSeal, false))
            """ + OFFERS_FILTER + " ORDER BY o.id DESC",
            countQuery = "SELECT count(o) " + OFFERS_FILTER)
    Page<ModerationOfferRow> findOffers(@Param("statuses") Collection<OfferStatus> statuses,
                                        @Param("reportedOnly") boolean reportedOnly,
                                        @Param("pattern") String pattern,
                                        Pageable page);

    @Query(value = """
            SELECT new com.geopslabs.geops.catalog.infrastructure.persistence.ModerationHistoryRow(
                   a.id, a.offerId, o.title, a.adminId, a.reason, a.removedAt)
            FROM ModerationActionJpaEntity a
            JOIN OfferJpaEntity o ON o.id = a.offerId
            ORDER BY a.removedAt DESC, a.id DESC
            """,
            countQuery = "SELECT count(a) FROM ModerationActionJpaEntity a")
    Page<ModerationHistoryRow> findHistory(Pageable page);
}
