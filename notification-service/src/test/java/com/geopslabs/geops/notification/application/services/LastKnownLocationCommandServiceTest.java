package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.domain.models.GeoPoint;
import com.geopslabs.geops.notification.domain.models.LastKnownLocation;
import com.geopslabs.geops.notification.domain.models.NotificationPreference;
import com.geopslabs.geops.notification.domain.models.commands.RecordLocationCommand;
import com.geopslabs.geops.notification.domain.models.exceptions.InvalidCaptureTimeException;
import com.geopslabs.geops.notification.domain.models.exceptions.RecipientNotFoundException;
import com.geopslabs.geops.notification.domain.ports.LastKnownLocationRepositoryPort;
import com.geopslabs.geops.notification.domain.ports.NotificationPreferenceRepositoryPort;
import com.geopslabs.geops.notification.domain.ports.RecipientRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LastKnownLocationCommandServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-08T13:05:00Z");
    private static final Long CONSUMER_ID = 1L;
    private static final double LATITUDE = -12.1211;
    private static final double LONGITUDE = -77.0297;
    private static final int ACCURACY = 25;
    private static final Instant JUST_CAPTURED = Instant.parse("2026-10-08T13:04:30Z");
    private static final Instant THREE_MINUTES_AHEAD = Instant.parse("2026-10-08T13:08:00Z");
    private static final Instant OLDER_CAPTURE = Instant.parse("2026-10-08T12:50:00Z");
    private static final LocalDateTime NEWER_STORED = LocalDateTime.parse("2026-10-08T13:00:00");

    @Mock
    private RecipientRepositoryPort recipientRepository;
    @Mock
    private NotificationPreferenceRepositoryPort preferenceRepository;
    @Mock
    private LastKnownLocationRepositoryPort locationRepository;

    private LastKnownLocationCommandService service;

    @BeforeEach
    void setUp() {
        service = new LastKnownLocationCommandService(recipientRepository, preferenceRepository, locationRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void storesTheReadingWithTheDefaultRadiusAndAnswersItIsFresh() {
        when(recipientRepository.existsConsumer(CONSUMER_ID)).thenReturn(true);
        when(locationRepository.saveIfNewer(eq(CONSUMER_ID), any())).thenAnswer(invocation -> invocation.getArgument(1));

        var recorded = service.record(command(JUST_CAPTURED));

        assertThat(recorded.location().radiusMeters()).isEqualTo(LastKnownLocationCommandService.DEFAULT_RADIUS_METERS);
        assertThat(recorded.location().position()).isEqualTo(new GeoPoint(LATITUDE, LONGITUDE));
        assertThat(recorded.fresh()).isTrue();
    }

    @Test
    void createsTheInitialPreferenceWithBothChannelsOff() {
        when(recipientRepository.existsConsumer(CONSUMER_ID)).thenReturn(true);
        when(locationRepository.saveIfNewer(eq(CONSUMER_ID), any())).thenAnswer(invocation -> invocation.getArgument(1));

        service.record(command(JUST_CAPTURED));

        var initial = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(preferenceRepository).createIfAbsent(initial.capture());
        assertThat(initial.getValue().getPushEnabled()).isFalse();
        assertThat(initial.getValue().getEmailEnabled()).isFalse();
        assertThat(initial.getValue().getDailyLimit()).isEqualTo(NotificationPreference.INITIAL_DAILY_LIMIT);
    }

    @Test
    void aCaptureTimeThreeMinutesAheadIsRejected() {
        when(recipientRepository.existsConsumer(CONSUMER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.record(command(THREE_MINUTES_AHEAD)))
                .isInstanceOf(InvalidCaptureTimeException.class);
        verify(locationRepository, never()).saveIfNewer(any(), any());
    }

    @Test
    void anOlderReadingAnswersTheStoredOne() {
        when(recipientRepository.existsConsumer(CONSUMER_ID)).thenReturn(true);
        var stored = new LastKnownLocation(new GeoPoint(LATITUDE, LONGITUDE), ACCURACY,
                LastKnownLocationCommandService.DEFAULT_RADIUS_METERS, NEWER_STORED);
        when(locationRepository.saveIfNewer(eq(CONSUMER_ID), any())).thenReturn(stored);

        var recorded = service.record(command(OLDER_CAPTURE));

        assertThat(recorded.location()).isEqualTo(stored);
        assertThat(recorded.fresh()).isTrue();
    }

    @Test
    void aConsumerWithoutRecipientCopyIsNotFound() {
        when(recipientRepository.existsConsumer(CONSUMER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.record(command(JUST_CAPTURED)))
                .isInstanceOf(RecipientNotFoundException.class);
        verify(preferenceRepository, never()).createIfAbsent(any());
    }

    private static RecordLocationCommand command(Instant capturedAt) {
        return new RecordLocationCommand(CONSUMER_ID, LATITUDE, LONGITUDE, ACCURACY, capturedAt);
    }
}
