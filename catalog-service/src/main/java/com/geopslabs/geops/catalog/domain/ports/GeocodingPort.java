package com.geopslabs.geops.catalog.domain.ports;

import com.geopslabs.geops.catalog.domain.models.GeocodingResult;

/**
 * Turns an address of Lima or Callao into coordinates. It never throws: a geocoder that does not answer, or whose
 * circuit breaker is open, is UNAVAILABLE.
 */
public interface GeocodingPort {
    GeocodingResult geocode(String address);
}
