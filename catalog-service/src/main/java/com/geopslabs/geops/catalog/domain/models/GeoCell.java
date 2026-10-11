package com.geopslabs.geops.catalog.domain.models;

import java.util.Collection;
import java.util.Objects;

/**
 * A geohash cell of 6 characters (about 1.2 km by 0.6 km), the unit the catalog is cached by (Informe 4.3.2.4).
 * The encoding is the standard geohash, the same one PostGIS answers with ST_GeoHash, so a cell computed here and
 * one computed by the database name the same rectangle.
 */
public record GeoCell(String geohash) {
    public static final int PRECISION = 6;
    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
    private static final int BITS_PER_CHAR = 5;
    private static final int HIGHEST_BIT = 4;
    private static final double ENVELOPE_MARGIN = 1.01;
    private static final double ENVELOPE_EXTRA_METERS = 1.0;

    public GeoCell {
        Objects.requireNonNull(geohash);
        if (geohash.length() != PRECISION || !geohash.chars().allMatch(c -> BASE32.indexOf(c) >= 0)) {
            throw new IllegalArgumentException("A cell is a geohash of " + PRECISION + " characters");
        }
    }

    public static GeoCell containing(GeoPoint point) {
        return containing(point.latitude(), point.longitude());
    }

    static GeoCell containing(double latitude, double longitude) {
        var hash = new StringBuilder(PRECISION);
        double minLatitude = GeoPoint.MIN_LATITUDE;
        double maxLatitude = GeoPoint.MAX_LATITUDE;
        double minLongitude = GeoPoint.MIN_LONGITUDE;
        double maxLongitude = GeoPoint.MAX_LONGITUDE;
        var evenBit = true;
        var bit = 0;
        var character = 0;
        while (hash.length() < PRECISION) {
            if (evenBit) {
                var middle = (minLongitude + maxLongitude) / 2;
                if (longitude >= middle) {
                    character = (character << 1) | 1;
                    minLongitude = middle;
                } else {
                    character = character << 1;
                    maxLongitude = middle;
                }
            } else {
                var middle = (minLatitude + maxLatitude) / 2;
                if (latitude >= middle) {
                    character = (character << 1) | 1;
                    minLatitude = middle;
                } else {
                    character = character << 1;
                    maxLatitude = middle;
                }
            }
            evenBit = !evenBit;
            if (++bit == BITS_PER_CHAR) {
                hash.append(BASE32.charAt(character));
                bit = 0;
                character = 0;
            }
        }
        return new GeoCell(hash.toString());
    }

    /** The smallest circle around the union of the cells: lets the database use its spatial index. */
    public static Envelope envelopeOf(Collection<GeoCell> cells) {
        if (cells.isEmpty()) {
            throw new IllegalArgumentException("No cells");
        }
        var bounds = cells.stream().map(GeoCell::bounds).reduce(Bounds::union).orElseThrow();
        var center = new GeoPoint((bounds.minLatitude + bounds.maxLatitude) / 2,
                (bounds.minLongitude + bounds.maxLongitude) / 2);
        var corner = new GeoPoint(bounds.maxLatitude, bounds.maxLongitude);
        var otherCorner = new GeoPoint(bounds.minLatitude, bounds.maxLongitude);
        var reach = Math.max(center.distanceTo(corner), center.distanceTo(otherCorner));
        return new Envelope(center, reach * ENVELOPE_MARGIN + ENVELOPE_EXTRA_METERS);
    }

    /** Distance from the point to the nearest point of the cell; zero when the point is inside. */
    public double distanceFrom(GeoPoint point) {
        var bounds = bounds();
        var nearest = new GeoPoint(clamp(point.latitude(), bounds.minLatitude, bounds.maxLatitude),
                clamp(point.longitude(), bounds.minLongitude, bounds.maxLongitude));
        return point.distanceTo(nearest);
    }

    public Bounds bounds() {
        double minLatitude = GeoPoint.MIN_LATITUDE;
        double maxLatitude = GeoPoint.MAX_LATITUDE;
        double minLongitude = GeoPoint.MIN_LONGITUDE;
        double maxLongitude = GeoPoint.MAX_LONGITUDE;
        var evenBit = true;
        for (var index = 0; index < geohash.length(); index++) {
            var value = BASE32.indexOf(geohash.charAt(index));
            for (var bit = HIGHEST_BIT; bit >= 0; bit--) {
                var isSet = ((value >> bit) & 1) == 1;
                if (evenBit) {
                    var middle = (minLongitude + maxLongitude) / 2;
                    if (isSet) {
                        minLongitude = middle;
                    } else {
                        maxLongitude = middle;
                    }
                } else {
                    var middle = (minLatitude + maxLatitude) / 2;
                    if (isSet) {
                        minLatitude = middle;
                    } else {
                        maxLatitude = middle;
                    }
                }
                evenBit = !evenBit;
            }
        }
        return new Bounds(minLatitude, maxLatitude, minLongitude, maxLongitude);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Bounds(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {
        Bounds union(Bounds other) {
            return new Bounds(Math.min(minLatitude, other.minLatitude), Math.max(maxLatitude, other.maxLatitude),
                    Math.min(minLongitude, other.minLongitude), Math.max(maxLongitude, other.maxLongitude));
        }

        public double height() {
            return maxLatitude - minLatitude;
        }

        public double width() {
            return maxLongitude - minLongitude;
        }
    }

    public record Envelope(GeoPoint center, double radiusMeters) {
    }
}
