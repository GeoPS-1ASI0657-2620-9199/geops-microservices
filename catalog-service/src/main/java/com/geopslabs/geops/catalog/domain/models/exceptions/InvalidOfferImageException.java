package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.DomainException;

public class InvalidOfferImageException extends DomainException {
    public static final String CODE = "INVALID_IMAGE";
    private static final int BYTES_PER_MEGABYTE = 1024 * 1024;

    private InvalidOfferImageException(String message) {
        super(CODE, message);
    }

    public static InvalidOfferImageException empty() {
        return new InvalidOfferImageException("Elige una foto para subir.");
    }

    public static InvalidOfferImageException tooLarge(int maxBytes) {
        return new InvalidOfferImageException("La foto pesa más de %d MB. Usa una más liviana."
                .formatted(maxBytes / BYTES_PER_MEGABYTE));
    }

    public static InvalidOfferImageException unsupportedFormat() {
        return new InvalidOfferImageException("La foto debe ser JPG, PNG o WebP.");
    }
}
