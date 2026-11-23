package com.geopslabs.geops.identity.domain.models;

/**
 * The RUC lookup did not answer (timeout, error, open circuit breaker or monthly limit reached). It is not an answer
 * about the RUC, so the verification stays pending instead of rejecting a taxpayer that may exist.
 */
public class TaxpayerRegistryUnavailableException extends RuntimeException {
    public TaxpayerRegistryUnavailableException(String reason) {
        super(reason);
    }
}
