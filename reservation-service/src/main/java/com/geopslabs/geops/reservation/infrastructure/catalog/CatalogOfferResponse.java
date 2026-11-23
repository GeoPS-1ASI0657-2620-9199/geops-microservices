package com.geopslabs.geops.reservation.infrastructure.catalog;

import java.time.LocalDate;

/**
 * Answer of GET /internal/v1/offers/{id}/availability. An offer that can be reserved must bring businessId, title
 * and validTo, because Reservation keeps them with the reservation. An offer that cannot be reserved only needs
 * offerId and available: Catalog sends an offer of a public source with available false and no businessId.
 */
public record CatalogOfferResponse(Long offerId, Long businessId, String title, LocalDate validTo, Boolean available) {

    boolean isComplete() {
        if (offerId == null || available == null) {
            return false;
        }
        return !available || (businessId != null && title != null && validTo != null);
    }
}
