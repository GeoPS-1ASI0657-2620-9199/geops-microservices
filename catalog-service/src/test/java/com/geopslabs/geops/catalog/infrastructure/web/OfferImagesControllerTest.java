package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.UploadOfferImageUseCase;
import com.geopslabs.geops.catalog.domain.models.OfferImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OfferImagesControllerTest {
    private static final Instant NOW = Instant.parse("2026-11-22T15:00:00Z");
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

    private UploadOfferImageUseCase images;
    private OfferImagesController controller;

    @BeforeEach
    void setUp() {
        images = mock(UploadOfferImageUseCase.class);
        controller = new OfferImagesController(images);
    }

    @Test
    void answersCreatedWithThePublicPathOfThePhoto() throws Exception {
        var id = UUID.randomUUID();
        when(images.upload(eq(84L), any())).thenReturn(new OfferImage(id, 84L, OfferImage.JPEG, JPEG, NOW));

        var response = controller.upload(owner(84L), new MockMultipartFile("file", "menu.jpg", "image/jpeg", JPEG));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().imageId()).isEqualTo(id);
        assertThat(response.getBody().path()).isEqualTo("/api/v1/offers/images/" + id);
    }

    @Test
    void refusesATokenWithoutBusiness() {
        var file = new MockMultipartFile("file", "menu.jpg", "image/jpeg", JPEG);

        assertThatThrownBy(() -> controller.upload(owner(null), file)).isInstanceOf(TokenClaimsException.class);
    }

    @Test
    void servesThePhotoWithItsTypeAndALongCache() {
        var id = UUID.randomUUID();
        when(images.find(id)).thenReturn(Optional.of(new OfferImage(id, 84L, OfferImage.PNG, JPEG, NOW)));

        var response = controller.read(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getHeaders().getCacheControl()).contains("max-age=31536000", "public", "immutable");
        assertThat(response.getBody()).isEqualTo(JPEG);
    }

    @Test
    void answersNotFoundForAnUnknownPhoto() {
        when(images.find(any())).thenReturn(Optional.empty());

        assertThat(controller.read(UUID.randomUUID()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static Jwt owner(Long businessId) {
        var builder = Jwt.withTokenValue("t").header("alg", "none").subject("7");
        if (businessId != null) {
            builder.claim("businessId", businessId);
        }
        return builder.build();
    }
}
