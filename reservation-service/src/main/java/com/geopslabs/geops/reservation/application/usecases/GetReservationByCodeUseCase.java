package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByCodeQuery;

public interface GetReservationByCodeUseCase {
    Reservation getByCode(GetReservationByCodeQuery query);
}
