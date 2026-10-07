package com.geopslabs.geops.engagement.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface BusinessSnapshotJpaRepository extends JpaRepository<BusinessSnapshotJpaEntity, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO business_snapshots (business_id, business_name, updated_at)
            VALUES (:businessId, :businessName, :updatedAt)
            ON CONFLICT (business_id) DO UPDATE SET business_name = EXCLUDED.business_name,
                updated_at = EXCLUDED.updated_at""",
            nativeQuery = true)
    int upsert(@Param("businessId") Long businessId, @Param("businessName") String businessName,
               @Param("updatedAt") Instant updatedAt);
}
