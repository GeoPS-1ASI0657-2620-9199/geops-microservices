package com.geopslabs.geops.engagement.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface RedeemedReservationJpaRepository extends JpaRepository<RedeemedReservationJpaEntity, Long> {

    @Query(value = """
            SELECT r.* FROM redeemed_reservations r
            WHERE r.consumer_id = :consumerId AND r.business_id = :businessId
              AND NOT EXISTS (SELECT 1 FROM reviews v WHERE v.reservation_id = r.reservation_id)
            ORDER BY r.redeemed_at, r.reservation_id
            LIMIT 1""",
            nativeQuery = true)
    Optional<RedeemedReservationJpaEntity> findOldestUnreviewed(@Param("consumerId") Long consumerId,
                                                                @Param("businessId") Long businessId);

    boolean existsByConsumerIdAndBusinessId(Long consumerId, Long businessId);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO redeemed_reservations (reservation_id, consumer_id, business_id, redeemed_at)
            VALUES (:reservationId, :consumerId, :businessId, :redeemedAt)
            ON CONFLICT (reservation_id) DO NOTHING""",
            nativeQuery = true)
    int insertIfAbsent(@Param("reservationId") Long reservationId, @Param("consumerId") Long consumerId,
                       @Param("businessId") Long businessId, @Param("redeemedAt") Instant redeemedAt);
}
