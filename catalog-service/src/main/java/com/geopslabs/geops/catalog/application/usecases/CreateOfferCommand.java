package com.geopslabs.geops.catalog.application.usecases;

import java.math.BigDecimal;
import java.time.LocalDate;

/** regularPrice is optional: null when the merchant does not show the saving. */
public record CreateOfferCommand(String title, String conditions, BigDecimal price, BigDecimal regularPrice,
                                 LocalDate validTo, String category, String imageUrl) {
}
