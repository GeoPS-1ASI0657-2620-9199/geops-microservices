package com.geopslabs.geops.catalog.infrastructure.persistence;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
