package com.geopslabs.geops.catalog.domain.models.queries;

/**
 * category is optional: null or blank searches every category.
 */
public record SearchNearbyOffersQuery(double latitude, double longitude, int radiusMinutes, String category,
                                      int page, int size) {
}
