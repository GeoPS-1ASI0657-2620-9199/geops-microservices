package com.geopslabs.geops.catalog.application.usecases;

/**
 * Módulo de Reputación del Comercio: Reservation redeemed a reservation or changed a report of a business (US44).
 */
public interface ApplyComplianceEventUseCase {
    /** Outcome of applying the event; a repeated event is not an error. */
    Outcome apply(ComplianceEvent event);

    enum Outcome {
        APPLIED,
        DUPLICATE
    }
}
