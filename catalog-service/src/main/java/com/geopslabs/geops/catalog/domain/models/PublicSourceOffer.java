package com.geopslabs.geops.catalog.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An offer of a public source with coordinates, as the separate search lists it (E2-08): the source instead of a
 * business and never a verified seal.
 */
public record PublicSourceOffer(Long offerId, String title, String sourceName, BigDecimal price, LocalDate validTo,
                                String category, String address, String imageUrl, GeoPoint location) {
}
