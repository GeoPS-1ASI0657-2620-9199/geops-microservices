package com.geopslabs.geops.engagement.infrastructure.messaging;

/**
 * The record cannot be read as an event: broken JSON, a missing field, an eventId that is not a UUID, a date that
 * cannot be parsed or a version newer than the consumer knows. Retrying will never fix it, so the error handler
 * sends it to the dead letter topic at once.
 */
public class UnreadableEventException extends RuntimeException {
    public UnreadableEventException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnreadableEventException(String message) {
        super(message);
    }
}
