package com.geopslabs.geops.notification.domain.models.exceptions;

import com.geopslabs.geops.notification.shared.domain.ForbiddenException;
public class PushSubscriptionOwnershipException extends ForbiddenException {
    public PushSubscriptionOwnershipException() { super("FORBIDDEN", "Este navegador ya está asociado a otra cuenta."); }
}
