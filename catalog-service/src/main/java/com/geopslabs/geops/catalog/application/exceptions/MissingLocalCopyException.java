package com.geopslabs.geops.catalog.application.exceptions;

/**
 * A local copy that an event needs has not arrived yet, for example the recipient of a notice whose UserRegistered is
 * still on its way. It is retried like any other failure and, if it keeps missing, ends in the dead letter topic
 * to be replayed.
 */
public class MissingLocalCopyException extends RuntimeException {
    public MissingLocalCopyException(String copyType, Object id) {
        super("Missing local copy %s %s".formatted(copyType, id));
    }
}
