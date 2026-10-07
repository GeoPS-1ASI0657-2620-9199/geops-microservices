package com.geopslabs.geops.notification.shared.domain;

public abstract class ForbiddenException extends DomainException {
    protected ForbiddenException(String code, String message) {
        super(code, message);
    }
}
