package com.geopslabs.geops.engagement.domain.models.exceptions;

import com.geopslabs.geops.engagement.shared.domain.ConflictException;

public class ReviewAlreadyExistsException extends ConflictException {
    private static final String CODE = "REVIEW_ALREADY_EXISTS";
    private static final String MESSAGE = "Ya comentaste cada canje que hiciste en este comercio.";

    public ReviewAlreadyExistsException() {
        super(CODE, MESSAGE);
    }
}
