package com.geopslabs.geops.engagement.domain.models.exceptions;

import com.geopslabs.geops.engagement.shared.domain.NotFoundException;
public class BusinessNotFoundException extends NotFoundException {
    public BusinessNotFoundException() { super("BUSINESS_NOT_FOUND", "No encontramos ese comercio."); }
}
