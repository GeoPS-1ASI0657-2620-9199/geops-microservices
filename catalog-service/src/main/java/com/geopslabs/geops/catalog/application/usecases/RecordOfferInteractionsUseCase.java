package com.geopslabs.geops.catalog.application.usecases;

import java.util.Collection;

/**
 * Módulo de Métricas: an offer was shown in the nearby search (impression) or its detail was opened (click). It
 * never fails nor waits for the database, so the search and the detail answer as fast as before.
 */
public interface RecordOfferInteractionsUseCase {
    /** For the services built without metrics, such as the unit tests of the search. */
    RecordOfferInteractionsUseCase NONE = new RecordOfferInteractionsUseCase() {
        @Override
        public void recordImpressions(Collection<Long> offerIds) {
            // Nothing to count.
        }

        @Override
        public void recordClick(Long offerId) {
            // Nothing to count.
        }
    };

    void recordImpressions(Collection<Long> offerIds);

    void recordClick(Long offerId);
}
