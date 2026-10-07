package com.geopslabs.geops.reservation.infrastructure.catalog;

import java.time.LocalDate;

public record CatalogOfferResponse(Long offerId, Long businessId, String title, LocalDate validTo, Boolean available) {

    boolean isComplete() {
        return offerId != null && businessId != null && title != null && validTo != null && available != null;
    }
}
