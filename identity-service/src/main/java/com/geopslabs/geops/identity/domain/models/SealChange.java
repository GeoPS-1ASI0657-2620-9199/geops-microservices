package com.geopslabs.geops.identity.domain.models;

/**
 * What a verification did to the verified seal. Catalog hears about every approval and every loss (BRD-09, RF-01):
 * a rejection of a business that never had the seal changes nothing there.
 */
public enum SealChange {
    GRANTED,
    CONFIRMED,
    REVOKED,
    UNCHANGED;

    public boolean mustBePublished() {
        return this != UNCHANGED;
    }
}
