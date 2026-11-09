package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.DomainException;

import java.util.List;

public class InvalidCategoryException extends DomainException {
    public static final String CODE = "INVALID_CATEGORY";
    private static final String MESSAGE = "La categoría no existe. Las categorías válidas son: %s";

    public InvalidCategoryException(List<String> validCategories) {
        super(CODE, MESSAGE.formatted(String.join(", ", validCategories)));
    }
}
