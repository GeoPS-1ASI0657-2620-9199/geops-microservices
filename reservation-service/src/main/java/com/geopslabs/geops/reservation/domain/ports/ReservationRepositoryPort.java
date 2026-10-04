package com.geopslabs.geops.reservation.domain.ports;

import com.geopslabs.geops.reservation.domain.models.Reservation;

import java.util.List;
import java.util.Optional;

public interface ReservationRepositoryPort {
    Reservation save(Reservation reservation);

    Optional<Reservation> findById(Long id);

    Optional<Reservation> findByCode(String code);

    List<Reservation> findAll();

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByPaymentId(Long paymentId);

    List<Reservation> findByPaymentCode(String paymentCode);

    List<Reservation> findByOfferId(Long offerId);

    List<Reservation> findByProductType(String productType);

    List<Reservation> findValidReservationsByUserId(Long userId, String currentTime);

    List<Reservation> findExpiredReservations(String currentTime);

    long countByUserId(Long userId);

    boolean existsByCode(String code);

    void deleteById(Long id);
}
