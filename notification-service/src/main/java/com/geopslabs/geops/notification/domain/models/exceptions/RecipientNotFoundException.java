package com.geopslabs.geops.notification.domain.models.exceptions;

import com.geopslabs.geops.notification.shared.domain.NotFoundException;

public class RecipientNotFoundException extends NotFoundException {
    private static final String CODE = "RECIPIENT_NOT_FOUND";
    private static final String MESSAGE = "Tu cuenta todavía no está lista para recibir avisos.";

    public RecipientNotFoundException() {
        super(CODE, MESSAGE);
    }
}
