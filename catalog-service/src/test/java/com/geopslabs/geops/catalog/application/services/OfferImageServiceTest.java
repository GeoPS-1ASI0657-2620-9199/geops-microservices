package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.domain.models.OfferImage;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidOfferImageException;
import com.geopslabs.geops.catalog.domain.ports.OfferImageRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OfferImageServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-22T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("America/Lima"));
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

    private OfferImageRepositoryPort images;
    private OfferImageService service;

    @BeforeEach
    void setUp() {
        images = mock(OfferImageRepositoryPort.class);
        when(images.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new OfferImageService(images, CLOCK);
    }

    @Test
    void storesTheValidatedPhotoOfTheBusinessWithTheTimeOfTheClock() {
        var image = service.upload(84L, JPEG);

        verify(images).save(image);
        assertThat(image.businessId()).isEqualTo(84L);
        assertThat(image.contentType()).isEqualTo(OfferImage.JPEG);
        assertThat(image.uploadedAt()).isEqualTo(NOW);
    }

    @Test
    void storesNothingWhenTheFileIsNotAPhoto() {
        assertThatThrownBy(() -> service.upload(84L, new byte[]{1, 2, 3}))
                .isInstanceOf(InvalidOfferImageException.class);
        verify(images, never()).save(any());
    }

    @Test
    void readsAPhotoByItsId() {
        var id = UUID.randomUUID();
        var stored = new OfferImage(id, 84L, OfferImage.JPEG, JPEG, NOW);
        when(images.findById(id)).thenReturn(Optional.of(stored));

        assertThat(service.find(id)).contains(stored);
        assertThat(service.find(UUID.randomUUID())).isEmpty();
    }
}
