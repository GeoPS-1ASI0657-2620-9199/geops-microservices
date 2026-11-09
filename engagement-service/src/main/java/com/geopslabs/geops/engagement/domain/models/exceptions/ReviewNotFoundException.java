package com.geopslabs.geops.engagement.domain.models.exceptions;

import com.geopslabs.geops.engagement.shared.domain.NotFoundException;
public class ReviewNotFoundException extends NotFoundException {
    public ReviewNotFoundException() { super("REVIEW_NOT_FOUND", "No encontramos ese comentario."); }
}
