package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidOfferImageException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfferImageTest {
    private static final Long BUSINESS_ID = 84L;
    private static final Instant NOW = Instant.parse("2026-11-22T15:00:00Z");
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
    static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);

    @Test
    void readsTheFormatFromTheBytes() {
        assertThat(OfferImage.upload(BUSINESS_ID, JPEG, NOW).contentType()).isEqualTo(OfferImage.JPEG);
        assertThat(OfferImage.upload(BUSINESS_ID, PNG, NOW).contentType()).isEqualTo(OfferImage.PNG);
        assertThat(OfferImage.upload(BUSINESS_ID, WEBP, NOW).contentType()).isEqualTo(OfferImage.WEBP);
    }

    @Test
    void rejectsAFileThatIsNotAPhotoWhateverItsName() {
        var pdf = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> OfferImage.upload(BUSINESS_ID, pdf, NOW))
                .isInstanceOf(InvalidOfferImageException.class)
                .hasMessageContaining("JPG, PNG o WebP");
    }

    @Test
    void rejectsAnEmptyFileAndOneOverTwoMegabytes() {
        var tooLarge = Arrays.copyOf(JPEG, OfferImage.MAX_BYTES + 1);

        assertThatThrownBy(() -> OfferImage.upload(BUSINESS_ID, new byte[0], NOW))
                .isInstanceOf(InvalidOfferImageException.class);
        assertThatThrownBy(() -> OfferImage.upload(BUSINESS_ID, tooLarge, NOW))
                .isInstanceOf(InvalidOfferImageException.class)
                .hasMessageContaining("2 MB");
    }

    @Test
    void acceptsExactlyTwoMegabytesAndKeepsItsOwnCopy() {
        var limit = Arrays.copyOf(JPEG, OfferImage.MAX_BYTES);

        var image = OfferImage.upload(BUSINESS_ID, limit, NOW);
        limit[0] = 0;

        assertThat(image.content()).hasSize(OfferImage.MAX_BYTES);
        assertThat(image.content()[0]).isEqualTo((byte) 0xFF);
        assertThat(image.businessId()).isEqualTo(BUSINESS_ID);
        assertThat(image.uploadedAt()).isEqualTo(NOW);
    }
}
