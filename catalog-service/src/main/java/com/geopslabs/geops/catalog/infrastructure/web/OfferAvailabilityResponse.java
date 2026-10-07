package com.geopslabs.geops.catalog.infrastructure.web;

import java.time.LocalDate;

public record OfferAvailabilityResponse(Long offerId, Long businessId, String title, LocalDate validTo,
                                        boolean available) {
}
