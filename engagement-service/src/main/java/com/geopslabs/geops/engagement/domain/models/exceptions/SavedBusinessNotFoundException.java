package com.geopslabs.geops.engagement.domain.models.exceptions;

import com.geopslabs.geops.engagement.shared.domain.NotFoundException;
public class SavedBusinessNotFoundException extends NotFoundException {
    public SavedBusinessNotFoundException() { super("SAVED_BUSINESS_NOT_FOUND", "No tienes guardado ese comercio."); }
}
