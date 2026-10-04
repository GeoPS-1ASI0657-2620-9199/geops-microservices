package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, Long> {

    List<ReservationJpaEntity> findByConsumerId(Long consumerId);

    Optional<ReservationJpaEntity> findByCode(String code);

    @Query("SELECT r FROM ReservationJpaEntity r WHERE r.consumerId = :consumerId AND r.expiresAt > :currentTime")
    List<ReservationJpaEntity> findValidReservationsByConsumerId(@Param("consumerId") Long consumerId,
                                                                 @Param("currentTime") Instant currentTime);

    Optional<ReservationJpaEntity> findByConsumerIdAndOfferIdAndStatus(Long consumerId, Long offerId,
                                                                       ReservationStatus status);

    boolean existsByCode(String code);
}
