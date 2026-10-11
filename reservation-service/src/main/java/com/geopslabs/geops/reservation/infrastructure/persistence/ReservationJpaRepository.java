package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, Long> {

    Optional<ReservationJpaEntity> findByCode(String code);

    List<ReservationJpaEntity> findByConsumerIdOrderByReservedAtDesc(Long consumerId);

    List<ReservationJpaEntity> findByConsumerIdAndStatusOrderByReservedAtDesc(Long consumerId,
                                                                              ReservationStatus status);

    Optional<ReservationJpaEntity> findByConsumerIdAndOfferIdAndStatus(Long consumerId, Long offerId,
                                                                       ReservationStatus status);

    boolean existsByCode(String code);

    /**
     * BRD-07, section 7.2: the state, the business and the deadline are checked by the same statement that
     * changes the row, so two concurrent redemptions of one code change it once.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE ReservationJpaEntity r
               SET r.status = :redeemed, r.redeemedAt = :now
             WHERE r.code = :code
               AND r.status = :active
               AND r.businessId = :businessId
               AND r.expiresAt > :now""")
    int redeemIfActive(@Param("code") String code, @Param("businessId") Long businessId, @Param("now") Instant now,
                       @Param("active") ReservationStatus active, @Param("redeemed") ReservationStatus redeemed);
}
