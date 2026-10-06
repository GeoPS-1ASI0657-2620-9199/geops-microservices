package com.geopslabs.geops.identity.shared;

public abstract class ConflictException extends DomainException {
    protected ConflictException(String code, String message) {
        super(code, message);
    }
}
