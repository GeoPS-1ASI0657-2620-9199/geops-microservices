package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.domain.events.EmailConfirmed;
import com.geopslabs.geops.identity.domain.models.Email;
import com.geopslabs.geops.identity.domain.models.EmailConfirmation;
import com.geopslabs.geops.identity.domain.models.InvalidEmailConfirmationException;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.ports.EmailConfirmationRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.OutboxPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
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
class EmailConfirmationServiceTest {
    private static final Instant REGISTERED_AT = Instant.parse("2026-10-27T14:00:00Z");
    private static final Instant CONFIRMED_AT = Instant.parse("2026-10-27T14:20:00Z");
    private static final UUID EVENT_ID = UUID.fromString("6e2a4c8d-1b3f-4a5e-9c7d-2f4b6d8e0a13");
    private static final Long USER_ID = 2001L;
    private static final String EMAIL = "lucia.fernandez@ejemplo.pe";
    private static final String TOKEN = "q3Zt8b1xV0mWc9yRkP2sLhN4dF6gJ7aE5uT0iO3pQ1w";

    @Mock
    private EmailConfirmationRepositoryPort emailConfirmations;
    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private OutboxPort outbox;

    private EmailConfirmationService service;

    @BeforeEach
    void setUp() {
        service = serviceAt(CONFIRMED_AT);
    }

    @Test
    void theLinkConfirmsTheEmailAndPublishesEmailConfirmed() {
        givenPendingConfirmation();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(null)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var event = ArgumentCaptor.forClass(EmailConfirmed.class);

        service.confirm(TOKEN);

        verify(outbox).append(event.capture());
        assertThat(event.getValue().eventId()).isEqualTo(EVENT_ID);
        assertThat(event.getValue().aggregateId()).isEqualTo(USER_ID);
        assertThat(event.getValue().payload().email()).isEqualTo(EMAIL);
        assertThat(event.getValue().payload().confirmedAt()).isEqualTo(CONFIRMED_AT);
        verify(emailConfirmations).deleteByUserId(USER_ID);
    }

    @Test
    void anUnknownOrUsedLinkIsRejected() {
        when(emailConfirmations.findByTokenHash(EmailConfirmation.hashOf(TOKEN))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(TOKEN)).isInstanceOf(InvalidEmailConfirmationException.class)
                .hasMessage("El enlace de confirmación no es válido o ya venció.");
        verify(outbox, never()).append(any());
    }

    @Test
    void anExpiredLinkIsRejected() {
        givenPendingConfirmation();
        service = serviceAt(REGISTERED_AT.plus(EmailConfirmation.VALIDITY).plus(Duration.ofSeconds(1)));

        assertThatThrownBy(() -> service.confirm(TOKEN)).isInstanceOf(InvalidEmailConfirmationException.class);
        verify(userRepository, never()).save(any());
        verify(outbox, never()).append(any());
    }

    @Test
    void anEmailAlreadyConfirmedDoesNotPublishTheEventAgain() {
        givenPendingConfirmation();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(REGISTERED_AT)));

        service.confirm(TOKEN);

        verify(userRepository, never()).save(any());
        verify(outbox, never()).append(any());
        verify(emailConfirmations).deleteByUserId(USER_ID);
    }

    private EmailConfirmationService serviceAt(Instant now) {
        return new EmailConfirmationService(emailConfirmations, userRepository, outbox,
                Clock.fixed(now, ZoneOffset.UTC), () -> EVENT_ID);
    }

    private void givenPendingConfirmation() {
        when(emailConfirmations.findByTokenHash(EmailConfirmation.hashOf(TOKEN)))
                .thenReturn(Optional.of(EmailConfirmation.issue(USER_ID, TOKEN, REGISTERED_AT)));
    }

    private static User user(Instant emailConfirmedAt) {
        return new User(USER_ID, User.register("Lucía Fernández Ríos", new Email(EMAIL), "987123456", "hash",
                Role.CONSUMER), emailConfirmedAt, 0, null, REGISTERED_AT);
    }
}
