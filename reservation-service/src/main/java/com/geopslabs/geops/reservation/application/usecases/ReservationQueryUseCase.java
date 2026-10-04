package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationsByConsumerIdQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByCodeQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByIdQuery;

import java.util.List;
import java.util.Optional;

public interface ReservationQueryUseCase {

    Optional<Reservation> handle(GetReservationByIdQuery query);

    List<Reservation> handle(GetReservationsByConsumerIdQuery query);

    Optional<Reservation> handle(GetReservationByCodeQuery query);

    List<Reservation> getAllReservations();

    List<Reservation> getValidReservationsByConsumerId(Long consumerId);

    List<Reservation> getExpiredReservations();
}
