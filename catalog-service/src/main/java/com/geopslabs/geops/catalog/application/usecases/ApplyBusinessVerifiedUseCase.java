package com.geopslabs.geops.catalog.application.usecases;

/**
 * Módulo de Reputación del Comercio: Identity verified (or stopped verifying) the RUC of a business.
 */
public interface ApplyBusinessVerifiedUseCase {
    /** Outcome of applying the event; never throws for a repeated or old event. */
    Outcome apply(BusinessVerifiedEvent event);

    enum Outcome {
        APPLIED,
        DUPLICATE,
        STALE
    }
}
