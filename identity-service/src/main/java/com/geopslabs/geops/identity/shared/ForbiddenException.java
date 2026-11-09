package com.geopslabs.geops.identity.shared;

public abstract class ForbiddenException extends DomainException {
    protected ForbiddenException(String code, String message) {
        super(code, message);
    }
}
