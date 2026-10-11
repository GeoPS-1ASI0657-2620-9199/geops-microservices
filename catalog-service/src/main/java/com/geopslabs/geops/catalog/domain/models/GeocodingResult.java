package com.geopslabs.geops.catalog.domain.models;

import java.util.Objects;

/**
 * What the geocoder said about an address: found with its point, not found (or found outside Lima and Callao), or
 * no answer at all. Only an UNAVAILABLE answer leaves the offer PENDING for the next run.
 */
public record GeocodingResult(Outcome outcome, GeoPoint point) {

    public enum Outcome {
        FOUND,
        NOT_FOUND,
        UNAVAILABLE
    }

    public GeocodingResult {
        Objects.requireNonNull(outcome);
        if ((outcome == Outcome.FOUND) != (point != null)) {
            throw new IllegalArgumentException("Only a found address carries a point");
        }
    }

    public static GeocodingResult found(GeoPoint point) {
        return new GeocodingResult(Outcome.FOUND, point);
    }

    public static GeocodingResult notFound() {
        return new GeocodingResult(Outcome.NOT_FOUND, null);
    }

    public static GeocodingResult unavailable() {
        return new GeocodingResult(Outcome.UNAVAILABLE, null);
    }
}
