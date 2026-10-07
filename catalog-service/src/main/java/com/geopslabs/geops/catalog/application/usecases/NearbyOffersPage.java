package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.RankedOffer;

import java.util.List;

public record NearbyOffersPage(List<RankedOffer> content, int page, long totalElements, int totalPages) {
}
