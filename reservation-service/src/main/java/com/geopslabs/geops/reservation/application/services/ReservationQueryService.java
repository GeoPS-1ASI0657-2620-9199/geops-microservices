package com.geopslabs.geops.reservation.application.services;

import com.geopslabs.geops.reservation.application.usecases.GetReservationByCodeUseCase;
import com.geopslabs.geops.reservation.application.usecases.GetReservationByIdUseCase;
import com.geopslabs.geops.reservation.application.usecases.ListConsumerReservationsUseCase;
import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationAccessDeniedException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationNotFoundException;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByCodeQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByIdQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationsByConsumerIdQuery;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ReservationQueryService
        implements GetReservationByIdUseCase, GetReservationByCodeUseCase, ListConsumerReservationsUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationQueryService.class);

    private final ReservationRepositoryPort reservationRepository;

    public ReservationQueryService(ReservationRepositoryPort reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    public Reservation getById(GetReservationByIdQuery query) {
        var reservation = reservationRepository.findById(query.reservationId())
                .orElseThrow(() -> ReservationNotFoundException.withId(query.reservationId()));
        if (!reservation.isOwnedByConsumer(query.consumerId())) {
            LOGGER.info("reservation.access-denied reservationId={} reason=other-consumer", reservation.getId());
            throw new ReservationAccessDeniedException(reservation.getId());
        }
        return reservation;
    }

    @Override
    public Reservation getByCode(GetReservationByCodeQuery query) {
        var reservation = reservationRepository.findByCode(query.code())
                .orElseThrow(() -> ReservationNotFoundException.withCode(query.code()));
        if (!reservation.isOfBusiness(query.businessId())) {
            LOGGER.info("reservation.access-denied reservationId={} reason=other-business", reservation.getId());
            throw new ReservationAccessDeniedException(reservation.getId());
        }
        return reservation;
    }

    @Override
    public List<Reservation> list(GetReservationsByConsumerIdQuery query) {
        return reservationRepository.findByConsumerId(query.consumerId(), query.status());
    }
}
