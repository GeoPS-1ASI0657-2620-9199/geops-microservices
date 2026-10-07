package com.geopslabs.geops.notification.shared.domain;

public abstract class ConflictException extends DomainException {
    protected ConflictException(String code, String message) {
        super(code, message);
    }
}
