package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
