package com.geopslabs.geops.engagement.domain.models.exceptions;

import com.geopslabs.geops.engagement.shared.domain.ForbiddenException;

public class RedemptionRequiredException extends ForbiddenException {
    private static final String CODE = "REDEMPTION_REQUIRED";
    private static final String MESSAGE = "Puedes comentar un comercio después de canjear una reserva en él.";

    public RedemptionRequiredException() {
        super(CODE, MESSAGE);
    }
}
