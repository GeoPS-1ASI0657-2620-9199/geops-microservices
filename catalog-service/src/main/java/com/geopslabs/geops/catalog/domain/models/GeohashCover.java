package com.geopslabs.geops.catalog.domain.models;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * The geohash cells of 6 characters that touch the circle of a search. It walks the grid of cells over the box of
 * the circle and keeps only the cells whose nearest point is inside the radius, so no cell is read for nothing.
 * With the 1 600 m of 20 walking minutes it gives between 6 and 15 cells in Lima.
 */
public final class GeohashCover {
    private static final double METERS_PER_DEGREE = Math.toRadians(1) * GeoPoint.EARTH_MEAN_RADIUS_METERS;
    private static final double HALF = 0.5;

    private GeohashCover() {
    }

    public static Set<GeoCell> cover(GeoPoint origin, int radiusMeters) {
        var latitudeSpan = radiusMeters / METERS_PER_DEGREE;
        var longitudeSpan = radiusMeters / (METERS_PER_DEGREE * Math.cos(Math.toRadians(origin.latitude())));
        var south = Math.max(GeoPoint.MIN_LATITUDE, origin.latitude() - latitudeSpan);
        var north = Math.min(GeoPoint.MAX_LATITUDE, origin.latitude() + latitudeSpan);
        var west = Math.max(GeoPoint.MIN_LONGITUDE, origin.longitude() - longitudeSpan);
        var east = Math.min(GeoPoint.MAX_LONGITUDE, origin.longitude() + longitudeSpan);

        var first = GeoCell.containing(south, west).bounds();
        var cells = new TreeSet<>(Comparator.comparing(GeoCell::geohash));
        for (var latitude = first.minLatitude(); latitude <= north; latitude += first.height()) {
            for (var longitude = first.minLongitude(); longitude <= east; longitude += first.width()) {
                var cell = GeoCell.containing(Math.min(GeoPoint.MAX_LATITUDE, latitude + first.height() * HALF),
                        Math.min(GeoPoint.MAX_LONGITUDE, longitude + first.width() * HALF));
                if (cell.distanceFrom(origin) <= radiusMeters) {
                    cells.add(cell);
                }
            }
        }
        return cells;
    }
}
