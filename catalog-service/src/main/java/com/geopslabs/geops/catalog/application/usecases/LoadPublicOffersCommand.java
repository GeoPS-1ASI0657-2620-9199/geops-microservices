package com.geopslabs.geops.catalog.application.usecases;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LoadPublicOffersCommand(List<PublicOffer> offers) {

    public record PublicOffer(String title, String conditions, BigDecimal price, LocalDate validTo, String category,
                              String address, String sourceName, String imageUrl) {
    }
}
