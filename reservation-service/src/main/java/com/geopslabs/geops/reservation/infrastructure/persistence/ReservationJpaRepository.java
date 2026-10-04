package com.geopslabs.geops.reservation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, Long> {

    List<ReservationJpaEntity> findByConsumerId(Long consumerId);

    Optional<ReservationJpaEntity> findByCode(String code);

    @Query("SELECT c FROM ReservationJpaEntity c WHERE c.consumerId = :consumerId AND " +
           "(c.expiresAt IS NULL OR c.expiresAt > :currentTime)")
    List<ReservationJpaEntity> findValidReservationsByConsumerId(@Param("consumerId") Long consumerId,
                                         @Param("currentTime") Instant currentTime);

    @Query("SELECT c FROM ReservationJpaEntity c WHERE c.expiresAt IS NOT NULL AND c.expiresAt <= :currentTime")
    List<ReservationJpaEntity> findExpiredReservations(@Param("currentTime") Instant currentTime);

    boolean existsByCode(String code);

}
