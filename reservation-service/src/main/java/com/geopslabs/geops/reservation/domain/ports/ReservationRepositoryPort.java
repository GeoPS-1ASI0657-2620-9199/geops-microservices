package com.geopslabs.geops.reservation.domain.ports;

import com.geopslabs.geops.reservation.domain.models.Reservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepositoryPort {
    Reservation save(Reservation reservation);

    Optional<Reservation> findById(Long id);

    Optional<Reservation> findByCode(String code);

    List<Reservation> findAll();

    List<Reservation> findByConsumerId(Long consumerId);

    List<Reservation> findValidReservationsByConsumerId(Long consumerId, LocalDateTime currentTime);

    Optional<Reservation> findActiveByConsumerAndOffer(Long consumerId, Long offerId);

    boolean existsByCode(String code);
}
