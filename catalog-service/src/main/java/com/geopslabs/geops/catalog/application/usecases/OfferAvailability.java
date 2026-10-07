package com.geopslabs.geops.catalog.application.usecases;

import java.time.LocalDate;

public record OfferAvailability(Long offerId, Long businessId, String title, LocalDate validTo, boolean available) {
}
