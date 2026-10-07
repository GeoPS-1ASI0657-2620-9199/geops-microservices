package com.geopslabs.geops.identity.domain.models;

public enum Role {
    CONSUMER,
    BUSINESS_OWNER,
    ADMIN;

    public boolean isSelfRegistrable() {
        return this != ADMIN;
    }

    public boolean requiresBusinessProfile() {
        return this == BUSINESS_OWNER;
    }
}
