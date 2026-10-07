package com.geopslabs.geops.catalog.shared.domain;

public abstract class ForbiddenException extends DomainException {
    protected ForbiddenException(String code, String message) {
        super(code, message);
    }
}
