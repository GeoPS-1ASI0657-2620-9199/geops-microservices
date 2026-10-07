package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.domain.models.NotificationPreference;
import com.geopslabs.geops.notification.domain.models.commands.UpdatePreferencesCommand;
import com.geopslabs.geops.notification.domain.models.exceptions.RecipientNotFoundException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceCommandServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-08T13:05:00Z");
    private static final Long CONSUMER_ID = 1L;
    private static final int DAILY_LIMIT = 3;
    private static final int OTHER_DAILY_LIMIT = 5;

    @Mock
    private RecipientRepositoryPort recipientRepository;
    @Mock
    private NotificationPreferenceRepositoryPort preferenceRepository;

    private NotificationPreferenceCommandService service;

    @BeforeEach
    void setUp() {
        service = new NotificationPreferenceCommandService(recipientRepository, preferenceRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void storesTheChannelsAndTheDailyLimit() {
        when(recipientRepository.existsConsumer(CONSUMER_ID)).thenReturn(true);
        when(preferenceRepository.upsert(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var preference = service.update(new UpdatePreferencesCommand(CONSUMER_ID, false, true, DAILY_LIMIT));

        assertThat(preference.getPushEnabled()).isFalse();
        assertThat(preference.getEmailEnabled()).isTrue();
        assertThat(preference.getDailyLimit()).isEqualTo(DAILY_LIMIT);
        assertThat(preference.getUpdatedAt()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
    }

    @Test
    void replacesThePreviousPreferenceThroughTheUpsert() {
        when(recipientRepository.existsConsumer(CONSUMER_ID)).thenReturn(true);
        when(preferenceRepository.upsert(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(new UpdatePreferencesCommand(CONSUMER_ID, true, false, OTHER_DAILY_LIMIT));

        var stored = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(preferenceRepository).upsert(stored.capture());
        assertThat(stored.getValue().getConsumerId()).isEqualTo(CONSUMER_ID);
        assertThat(stored.getValue().getPushEnabled()).isTrue();
        assertThat(stored.getValue().getDailyLimit()).isEqualTo(OTHER_DAILY_LIMIT);
    }

    @Test
    void aConsumerWithoutRecipientCopyIsNotFoundAndNothingIsWritten() {
        when(recipientRepository.existsConsumer(CONSUMER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.update(new UpdatePreferencesCommand(CONSUMER_ID, true, true, DAILY_LIMIT)))
                .isInstanceOf(RecipientNotFoundException.class);
        verify(preferenceRepository, never()).upsert(any());
    }
}
