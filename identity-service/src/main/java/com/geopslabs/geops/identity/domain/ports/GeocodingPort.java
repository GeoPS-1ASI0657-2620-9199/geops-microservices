package com.geopslabs.geops.identity.domain.ports;

import com.geopslabs.geops.identity.domain.models.GeocodingResult;

/**
 * Checks that an address exists in Lima or Callao. It never throws: a geocoder that does not answer is UNAVAILABLE.
 */
public interface GeocodingPort {
    GeocodingResult geocode(String address);
}
