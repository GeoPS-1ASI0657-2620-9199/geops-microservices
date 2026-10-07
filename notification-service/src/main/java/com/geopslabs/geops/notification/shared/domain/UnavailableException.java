package com.geopslabs.geops.notification.shared.domain;

public abstract class UnavailableException extends DomainException {
    protected UnavailableException(String code, String message) {
        super(code, message);
    }
}
