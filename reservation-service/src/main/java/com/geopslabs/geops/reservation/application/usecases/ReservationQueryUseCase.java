package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByCodeQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByIdQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationsByConsumerIdQuery;

import java.util.List;

public interface ReservationQueryUseCase {
    Reservation handle(GetReservationByIdQuery query);

    List<Reservation> handle(GetReservationsByConsumerIdQuery query);

    Reservation handle(GetReservationByCodeQuery query);

    List<Reservation> getAllReservations();

    List<Reservation> getValidReservationsByConsumerId(Long consumerId);
}
