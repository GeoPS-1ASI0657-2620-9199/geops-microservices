package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidOfferImageException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Photo of an offer uploaded by its business. Only JPEG, PNG and WebP up to 2 MB, and the format is read from the
 * first bytes of the file, never trusted from the declared content type.
 */
public record OfferImage(UUID imageId, Long businessId, String contentType, byte[] content, Instant uploadedAt) {
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    public static final String JPEG = "image/jpeg";
    public static final String PNG = "image/png";
    public static final String WEBP = "image/webp";

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] RIFF = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MARK = {0x57, 0x45, 0x42, 0x50};
    private static final int WEBP_MARK_OFFSET = 8;

    public OfferImage {
        Objects.requireNonNull(imageId, "imageId");
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(uploadedAt, "uploadedAt");
    }

    /** Validates the bytes and builds the image with the format they really have. */
    public static OfferImage upload(Long businessId, byte[] content, Instant uploadedAt) {
        if (content == null || content.length == 0) {
            throw InvalidOfferImageException.empty();
        }
        if (content.length > MAX_BYTES) {
            throw InvalidOfferImageException.tooLarge(MAX_BYTES);
        }
        return new OfferImage(UUID.randomUUID(), businessId, formatOf(content), content.clone(), uploadedAt);
    }

    private static String formatOf(byte[] content) {
        if (startsWith(content, JPEG_SIGNATURE, 0)) {
            return JPEG;
        }
        if (startsWith(content, PNG_SIGNATURE, 0)) {
            return PNG;
        }
        if (startsWith(content, RIFF, 0) && startsWith(content, WEBP_MARK, WEBP_MARK_OFFSET)) {
            return WEBP;
        }
        throw InvalidOfferImageException.unsupportedFormat();
    }

    private static boolean startsWith(byte[] content, byte[] signature, int offset) {
        return content.length >= offset + signature.length
                && Arrays.equals(content, offset, offset + signature.length, signature, 0, signature.length);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OfferImage image && imageId.equals(image.imageId);
    }

    @Override
    public int hashCode() {
        return imageId.hashCode();
    }

    @Override
    public String toString() {
        return "OfferImage[imageId=%s, businessId=%d, contentType=%s, bytes=%d]"
                .formatted(imageId, businessId, contentType, content.length);
    }
}
