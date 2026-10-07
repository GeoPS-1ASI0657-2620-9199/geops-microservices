package com.geopslabs.geops.catalog.domain.models.queries;

public record SearchNearbyOffersQuery(double latitude, double longitude, int radiusMinutes, int page, int size) {
}
