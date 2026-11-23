package com.geopslabs.geops.catalog.application.usecases;

/** Módulo de Métricas: Reservation created or redeemed a reservation of an offer (US11). */
public interface ApplyReservationMetricEventUseCase {
    Outcome apply(ReservationMetricEvent event);

    enum Outcome {
        APPLIED,
        DUPLICATE,
        /** The offer is not of a campaign (a public source) or is not in the catalog. */
        NOT_COUNTED
    }
}
