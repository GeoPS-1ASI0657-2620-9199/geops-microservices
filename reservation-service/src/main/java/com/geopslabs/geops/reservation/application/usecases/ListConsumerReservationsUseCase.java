package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationsByConsumerIdQuery;

import java.util.List;

public interface ListConsumerReservationsUseCase {
    List<Reservation> list(GetReservationsByConsumerIdQuery query);
}
