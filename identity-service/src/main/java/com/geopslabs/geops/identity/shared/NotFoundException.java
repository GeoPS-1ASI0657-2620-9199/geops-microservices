package com.geopslabs.geops.identity.shared;

public abstract class NotFoundException extends DomainException {
    protected NotFoundException(String code, String message) {
        super(code, message);
    }
}
