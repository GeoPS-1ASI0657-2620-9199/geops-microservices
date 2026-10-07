package com.geopslabs.geops.catalog.domain.ports;

import com.geopslabs.geops.catalog.domain.models.GeoPoint;

public interface GeocodingPort {
    GeoPoint geocode(String address);
}
