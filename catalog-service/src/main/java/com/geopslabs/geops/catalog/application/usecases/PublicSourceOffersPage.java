package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.PublicSourceOffer;

import java.util.List;

public record PublicSourceOffersPage(List<Ranked> content, int page, long totalElements, int totalPages) {

    public record Ranked(PublicSourceOffer offer, double distanceMeters, int walkMinutes) {
    }
}
