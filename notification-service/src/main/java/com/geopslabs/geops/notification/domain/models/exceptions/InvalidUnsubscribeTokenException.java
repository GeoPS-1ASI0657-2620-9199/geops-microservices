package com.geopslabs.geops.notification.domain.models.exceptions;

import com.geopslabs.geops.notification.shared.domain.RuleViolationException;
public class InvalidUnsubscribeTokenException extends RuleViolationException {
    public InvalidUnsubscribeTokenException() { super("INVALID_UNSUBSCRIBE_TOKEN", "El enlace de baja no es válido."); }
}
