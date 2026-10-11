package com.geopslabs.geops.catalog.infrastructure.messaging;

/**
 * The record on identity.events cannot be read as an event: broken JSON, a missing field, an eventId that is not a
 * UUID or a date that cannot be parsed. Retrying will never fix it, so it is the only failure the error handler
 * discards (E2-03). Any other exception, a bug included, is retried and holds the partition.
 */
public class UnreadableEventException extends RuntimeException {
    public UnreadableEventException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnreadableEventException(String message) {
        super(message);
    }
}
