package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.NotFoundException;

public class BusinessNotFoundException extends NotFoundException {
    private static final String CODE = "BUSINESS_NOT_FOUND";
    private static final String MESSAGE = "Business %d is not in the catalog yet";

    public BusinessNotFoundException(Long businessId) {
        super(CODE, MESSAGE.formatted(businessId));
    }
}
