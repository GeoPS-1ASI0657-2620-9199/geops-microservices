package com.geopslabs.geops.catalog.domain.models.queries;

/**
 * category is optional: null or blank searches every category. text is optional too: null or blank applies no
 * text filter.
 */
public record SearchNearbyOffersQuery(double latitude, double longitude, int radiusMinutes, String category,
                                      String text, int page, int size) {

    public SearchNearbyOffersQuery(double latitude, double longitude, int radiusMinutes, String category,
                                   int page, int size) {
        this(latitude, longitude, radiusMinutes, category, null, page, size);
    }
}
