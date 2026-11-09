package com.geopslabs.geops.engagement.infrastructure.persistence;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
