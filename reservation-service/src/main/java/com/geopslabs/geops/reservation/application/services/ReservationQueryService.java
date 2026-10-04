package com.geopslabs.geops.reservation.application.services;

import com.geopslabs.geops.reservation.application.usecases.ReservationQueryUseCase;
import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationNotFoundException;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByCodeQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByIdQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationsByConsumerIdQuery;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class ReservationQueryService implements ReservationQueryUseCase {
    private final ReservationRepositoryPort reservationRepository;

    public ReservationQueryService(ReservationRepositoryPort reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    public Reservation handle(GetReservationByIdQuery query) {
        return reservationRepository.findById(query.reservationId())
                .orElseThrow(() -> ReservationNotFoundException.withId(query.reservationId()));
    }

    @Override
    public List<Reservation> handle(GetReservationsByConsumerIdQuery query) {
        return reservationRepository.findByConsumerId(query.consumerId());
    }

    @Override
    public Reservation handle(GetReservationByCodeQuery query) {
        return reservationRepository.findByCode(query.code())
                .orElseThrow(() -> ReservationNotFoundException.withCode(query.code()));
    }

    @Override
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @Override
    public List<Reservation> getValidReservationsByConsumerId(Long consumerId) {
        return reservationRepository.findValidReservationsByConsumerId(consumerId, LocalDateTime.now(ZoneOffset.UTC));
    }
}
