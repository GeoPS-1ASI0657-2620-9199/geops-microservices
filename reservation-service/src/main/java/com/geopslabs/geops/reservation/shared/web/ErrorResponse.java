package com.geopslabs.geops.reservation.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * {@code redeemedAt} only travels with RESERVATION_ALREADY_REDEEMED; every other error has code and message.
 */
public record ErrorResponse(String code, String message,
                            @JsonInclude(JsonInclude.Include.NON_NULL) Instant redeemedAt) {

    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }
}
