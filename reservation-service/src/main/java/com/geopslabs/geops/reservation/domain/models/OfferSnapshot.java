package com.geopslabs.geops.reservation.domain.models;

import java.time.LocalDate;

public record OfferSnapshot(Long offerId, Long businessId, String title, LocalDate validTo) {
}
