package com.geopslabs.geops.reservation.application.services;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationsByConsumerIdQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByCodeQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByIdQuery;
import com.geopslabs.geops.reservation.application.usecases.ReservationQueryUseCase;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
public class ReservationQueryService implements ReservationQueryUseCase {

    private final ReservationRepositoryPort reservationRepository;

    public ReservationQueryService(ReservationRepositoryPort reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    public Optional<Reservation> handle(GetReservationByIdQuery query) {
        try {
            return reservationRepository.findById(query.reservationId());
        } catch (Exception e) {
            System.err.println("Error retrieving reservation by ID: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<Reservation> handle(GetReservationsByConsumerIdQuery query) {
        try {
            return reservationRepository.findByConsumerId(query.consumerId());
        } catch (Exception e) {
            System.err.println("Error retrieving reservations by user ID: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<Reservation> handle(GetReservationByCodeQuery query) {
        try {
            return reservationRepository.findByCode(query.code());
        } catch (Exception e) {
            System.err.println("Error retrieving reservation by code: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<Reservation> getAllReservations() {
        try {
            return reservationRepository.findAll();
        } catch (Exception e) {
            System.err.println("Error retrieving all reservations: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<Reservation> getValidReservationsByConsumerId(Long consumerId) {
        if (consumerId == null) {
            throw new IllegalArgumentException("consumerId cannot be null or empty");
        }

        try {
            LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);
            return reservationRepository.findValidReservationsByConsumerId(consumerId, currentTime);
        } catch (Exception e) {
            System.err.println("Error retrieving valid reservations by user ID: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<Reservation> getExpiredReservations() {
        try {
            LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);
            return reservationRepository.findExpiredReservations(currentTime);
        } catch (Exception e) {
            System.err.println("Error retrieving expired reservations: " + e.getMessage());
            return List.of();
        }
    }

}
