package com.geopslabs.geops.reservation.shared.domain;

public abstract class InvalidRequestException extends DomainException {
    protected InvalidRequestException(String code, String message) {
        super(code, message);
    }
}
