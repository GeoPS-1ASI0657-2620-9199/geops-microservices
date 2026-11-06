package com.geopslabs.geops.engagement.domain.models.exceptions;

import com.geopslabs.geops.engagement.shared.domain.ConflictException;
public class ReplyAlreadyExistsException extends ConflictException {
    public ReplyAlreadyExistsException() { super("REPLY_ALREADY_EXISTS", "Este comentario ya tiene una respuesta."); }
}
