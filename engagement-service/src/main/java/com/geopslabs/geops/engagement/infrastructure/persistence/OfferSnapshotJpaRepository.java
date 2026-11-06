package com.geopslabs.geops.engagement.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

public interface OfferSnapshotJpaRepository extends JpaRepository<OfferSnapshotJpaEntity, Long> {

    @Query("select o from OfferSnapshotJpaEntity o where o.businessId in :ids and o.status = 'PUBLISHED' and o.validTo >= :today order by o.offerId")
    java.util.List<OfferSnapshotJpaEntity> findAvailable(@Param("ids") java.util.Collection<Long> ids, @Param("today") LocalDate today);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO offer_snapshots (offer_id, business_id, title, valid_to, status, updated_at)
            VALUES (:offerId, :businessId, :title, :validTo, :status, :updatedAt)
            ON CONFLICT (offer_id) DO UPDATE SET business_id = EXCLUDED.business_id, title = EXCLUDED.title,
                valid_to = EXCLUDED.valid_to, status = EXCLUDED.status, updated_at = EXCLUDED.updated_at""",
            nativeQuery = true)
    int upsert(@Param("offerId") Long offerId, @Param("businessId") Long businessId, @Param("title") String title,
               @Param("validTo") LocalDate validTo, @Param("status") String status,
               @Param("updatedAt") Instant updatedAt);
}
