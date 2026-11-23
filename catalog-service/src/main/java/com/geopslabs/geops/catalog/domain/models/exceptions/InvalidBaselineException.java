package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.DomainException;

public class InvalidBaselineException extends DomainException {
    public static final String CODE = "INVALID_BASELINE";

    private InvalidBaselineException(String message) {
        super(CODE, message);
    }

    public static InvalidBaselineException customersPerWeek(int max) {
        return new InvalidBaselineException("Los clientes por semana deben estar entre 1 y %d.".formatted(max));
    }

    public static InvalidBaselineException averageTicket() {
        return new InvalidBaselineException(
                "El ticket promedio debe ser mayor que 0, menor que 100000 y tener a lo más dos decimales.");
    }
}
