package com.geopslabs.geops.identity.infrastructure.persistence;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
