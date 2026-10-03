package com.geopslabs.geops.identity.shared;

public abstract class UnauthenticatedException extends DomainException {
    protected UnauthenticatedException(String code, String message) {
        super(code, message);
    }
}
