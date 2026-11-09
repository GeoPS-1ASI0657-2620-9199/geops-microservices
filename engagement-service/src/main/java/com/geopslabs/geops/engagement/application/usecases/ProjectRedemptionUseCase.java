package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.RedeemedReservation;
public interface ProjectRedemptionUseCase {
    void reservationRedeemed(RedeemedReservation reservation);
}
