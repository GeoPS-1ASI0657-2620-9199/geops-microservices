package com.geopslabs.geops.reservation.shared.domain;

public abstract class ForbiddenException extends DomainException {
    protected ForbiddenException(String code, String message) {
        super(code, message);
    }
}
