package com.geopslabs.geops.notification.application.exceptions;

/**
 * The circuit breaker of a delivery provider is open or its limit of calls in flight is full (US34): the provider
 * was not even called. The notice stays pending; the event that needed it is retried like a database outage, with
 * no limit, until the circuit closes again.
 */
public class ProviderUnavailableException extends RuntimeException {
    public ProviderUnavailableException(String provider, String reason) {
        super("Provider %s unavailable: %s".formatted(provider, reason));
    }
}
