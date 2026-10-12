package com.geopslabs.geops.catalog.domain.models;

/**
 * GEOCODED: the offer has coordinates. PENDING: a public source offer that waits for Nominatim (US07), never shown
 * in the search. MANUAL_REVIEW: Nominatim did not find the address, or found it outside Lima and Callao.
 */
public enum GeocodingStatus {
    GEOCODED,
    PENDING,
    MANUAL_REVIEW
}
