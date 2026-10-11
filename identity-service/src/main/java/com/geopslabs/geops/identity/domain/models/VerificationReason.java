package com.geopslabs.geops.identity.domain.models;

/**
 * Why a verification was not approved. The first four reject it (BRD-09, RF-01); GEOCODER_UNAVAILABLE leaves it
 * pending until Nominatim answers again.
 */
public enum VerificationReason {
    RUC_NOT_FOUND,
    TAXPAYER_NOT_ACTIVE,
    TAXPAYER_NOT_HABIDO,
    ADDRESS_NOT_GEOCODABLE,
    GEOCODER_UNAVAILABLE
}
