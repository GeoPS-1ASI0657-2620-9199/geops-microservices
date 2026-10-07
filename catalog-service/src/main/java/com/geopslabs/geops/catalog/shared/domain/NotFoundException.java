package com.geopslabs.geops.catalog.shared.domain;

public abstract class NotFoundException extends DomainException {
    protected NotFoundException(String code, String message) {
        super(code, message);
    }
}
