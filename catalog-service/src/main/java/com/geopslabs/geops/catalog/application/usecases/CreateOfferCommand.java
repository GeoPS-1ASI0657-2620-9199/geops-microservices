package com.geopslabs.geops.catalog.application.usecases;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateOfferCommand(String title, String conditions, BigDecimal price, LocalDate validTo,
                                 String category, String imageUrl) {
}
