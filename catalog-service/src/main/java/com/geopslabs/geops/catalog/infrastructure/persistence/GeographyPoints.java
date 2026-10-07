package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

final class GeographyPoints {
    static final String GEOGRAPHY_POINT = "geography(Point,4326)";
    private static final int WGS84 = 4326;
    private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), WGS84);

    private GeographyPoints() {
    }

    static GeoPoint toGeoPoint(Point point) {
        return point == null ? null : new GeoPoint(point.getY(), point.getX());
    }

    static Point toPoint(GeoPoint location) {
        return location == null ? null
                : GEOMETRY.createPoint(new Coordinate(location.longitude(), location.latitude()));
    }
}
