package com.geopslabs.geops.reservation.shared.domain;

public abstract class GoneException extends DomainException {
    protected GoneException(String code, String message) {
        super(code, message);
    }
}
