package com.geopslabs.geops.reservation.infrastructure.persistence;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
