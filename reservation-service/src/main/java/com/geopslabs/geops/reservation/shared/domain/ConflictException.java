package com.geopslabs.geops.reservation.shared.domain;

public abstract class ConflictException extends DomainException {
    protected ConflictException(String code, String message) {
        super(code, message);
    }
}
