package com.geopslabs.geops.engagement.domain.models.exceptions;

import com.geopslabs.geops.engagement.shared.domain.ForbiddenException;
public class ReviewOwnershipException extends ForbiddenException {
    public ReviewOwnershipException() { super("FORBIDDEN", "Solo el comercio del comentario puede responder."); }
}
