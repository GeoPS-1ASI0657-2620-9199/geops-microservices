package com.geopslabs.geops.reservation.domain.ports;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepositoryPort {
    Reservation save(Reservation reservation);

    Optional<Reservation> findById(Long id);

    Optional<Reservation> findByCode(String code);

    List<Reservation> findByConsumerId(Long consumerId, ReservationStatus status);

    Optional<Reservation> findActiveByConsumerAndOffer(Long consumerId, Long offerId);

    boolean existsByCode(String code);

    /**
     * Single conditional statement: ACTIVE, of this business and with {@code expires_at > now}.
     *
     * @return the number of rows changed, 1 when this call redeemed the reservation and 0 otherwise
     */
    int redeemIfActive(String code, Long businessId, LocalDateTime now);
}
