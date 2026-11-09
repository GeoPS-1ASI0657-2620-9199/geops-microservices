package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.domain.events.ConsumerPreferencesChanged;
import com.geopslabs.geops.identity.domain.events.DomainEvent;
import com.geopslabs.geops.identity.domain.models.ConsumerProfile;
import com.geopslabs.geops.identity.domain.models.ConsumerProfileNotFoundException;
import com.geopslabs.geops.identity.domain.models.SearchRadiusOutOfRangeException;
import com.geopslabs.geops.identity.domain.ports.ConsumerProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.OutboxPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumerProfileServiceTest {
    private static final Long CONSUMER_ID = 41L;
    private static final Instant NOW = Instant.parse("2026-10-21T15:04:00Z");
    private static final UUID EVENT_ID = UUID.fromString("5b0d7a52-0d4a-4f87-9a54-0f1c7a2b9e11");

    @Mock
    private ConsumerProfileRepositoryPort consumerProfiles;
    @Mock
    private OutboxPort outbox;

    private ConsumerProfileService service;

    @BeforeEach
    void setUp() {
        service = new ConsumerProfileService(consumerProfiles, outbox, Clock.fixed(NOW, ZoneOffset.UTC),
                () -> EVENT_ID);
    }

    @Test
    void savesTheRadiusAndWritesConsumerPreferencesChanged() {
        when(consumerProfiles.findByUserId(CONSUMER_ID)).thenReturn(Optional.of(new ConsumerProfile(7L,
                ConsumerProfile.createFor(CONSUMER_ID))));
        when(consumerProfiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = service.changeSearchRadius(CONSUMER_ID, 15);

        assertThat(saved.getSearchRadiusMinutes()).isEqualTo(15);
        var event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outbox).append(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(ConsumerPreferencesChanged.EVENT_TYPE);
        assertThat(event.getValue().aggregateId()).isEqualTo(CONSUMER_ID);
        assertThat(event.getValue().payload()).isEqualTo(new ConsumerPreferencesChanged.Payload(CONSUMER_ID, 15));
    }

    @Test
    void theSameRadiusPublishesNothing() {
        when(consumerProfiles.findByUserId(CONSUMER_ID)).thenReturn(Optional.of(ConsumerProfile.createFor(CONSUMER_ID)));

        service.changeSearchRadius(CONSUMER_ID, 10);

        verify(consumerProfiles, never()).save(any());
        verify(outbox, never()).append(any());
    }

    @Test
    void aRadiusOutOfRangeIsNotSaved() {
        when(consumerProfiles.findByUserId(CONSUMER_ID)).thenReturn(Optional.of(ConsumerProfile.createFor(CONSUMER_ID)));

        assertThatThrownBy(() -> service.changeSearchRadius(CONSUMER_ID, 25))
                .isInstanceOf(SearchRadiusOutOfRangeException.class);
        verify(outbox, never()).append(any());
    }

    @Test
    void aConsumerWithoutProfileIsNotFound() {
        when(consumerProfiles.findByUserId(CONSUMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(CONSUMER_ID)).isInstanceOf(ConsumerProfileNotFoundException.class);
    }
}
