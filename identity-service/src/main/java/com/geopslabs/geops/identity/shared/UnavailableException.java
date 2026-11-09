package com.geopslabs.geops.identity.shared;

public abstract class UnavailableException extends DomainException {
    protected UnavailableException(String code, String message) {
        super(code, message);
    }
}
