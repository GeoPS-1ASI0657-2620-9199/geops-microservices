package com.geopslabs.geops.catalog.infrastructure.web;

import java.util.List;

public record NearbyOffersResponse(List<NearbyOfferResponse> content, int page, long totalElements, int totalPages) {
}
