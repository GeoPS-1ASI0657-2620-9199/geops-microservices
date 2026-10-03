package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.RegisterUserCommand;
import com.geopslabs.geops.identity.domain.models.ConsumerProfile;
import com.geopslabs.geops.identity.domain.models.EmailAlreadyRegisteredException;
import com.geopslabs.geops.identity.domain.models.PhoneAlreadyRegisteredException;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.RoleNotAllowedException;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.ConsumerProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.PasswordHasherPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {
    private static final Long USER_ID = 41L;
    private static final Long PROFILE_ID = 15L;
    private static final int NO_FAILED_ATTEMPTS = 0;
    private static final int DEFAULT_SEARCH_RADIUS_MINUTES = 10;
    private static final String FULL_NAME = "Lucía Fernández Ríos";
    private static final String EMAIL = "lucia.fernandez@ejemplo.pe";
    private static final String PHONE = "987123456";
    private static final String PASSWORD = "Ofertas#2026";
    private static final String PASSWORD_HASH = "$2a$10$hash";

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private ConsumerProfileRepositoryPort consumerProfileRepository;
    @Mock
    private BusinessProfileRepositoryPort businessProfileRepository;
    @Mock
    private PasswordHasherPort passwordHasher;

    private UserCommandService service;

    @BeforeEach
    void setUp() {
        service = new UserCommandService(userRepository, consumerProfileRepository, businessProfileRepository,
                passwordHasher);
    }

    @Test
    void registersConsumerWithItsProfile() {
        givenSavesSucceed();

        var registered = service.register(command(Role.CONSUMER, EMAIL));

        assertThat(registered.userId()).isEqualTo(USER_ID);
        assertThat(registered.role()).isEqualTo(Role.CONSUMER);
        assertThat(registered.consumerProfileId()).isEqualTo(PROFILE_ID);
    }

    @Test
    void storesOnlyThePasswordHash() {
        givenSavesSucceed();
        var savedUser = ArgumentCaptor.forClass(User.class);

        service.register(command(Role.CONSUMER, EMAIL));

        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(savedUser.getValue().getFailedLoginAttempts()).isEqualTo(NO_FAILED_ATTEMPTS);
    }

    @Test
    void createsTheConsumerProfileWithDefaultValues() {
        givenSavesSucceed();
        var savedProfile = ArgumentCaptor.forClass(ConsumerProfile.class);

        service.register(command(Role.CONSUMER, EMAIL));

        verify(consumerProfileRepository).save(savedProfile.capture());
        assertThat(savedProfile.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(savedProfile.getValue().isLocationPermission()).isFalse();
        assertThat(savedProfile.getValue().getSearchRadiusMinutes()).isEqualTo(DEFAULT_SEARCH_RADIUS_MINUTES);
    }

    @Test
    void normalizesTheEmailBeforeCheckingAndSaving() {
        givenSavesSucceed();

        var registered = service.register(command(Role.CONSUMER, "  Lucia.Fernandez@Ejemplo.PE "));

        verify(userRepository).existsByEmail(EMAIL);
        assertThat(registered.email()).isEqualTo(EMAIL);
    }

    @Test
    void rejectsDuplicatedEmail() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> service.register(command(Role.CONSUMER, EMAIL)))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessageNotContaining(FULL_NAME);
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicatedPhone() {
        when(userRepository.existsByPhone(PHONE)).thenReturn(true);

        assertThatThrownBy(() -> service.register(command(Role.CONSUMER, EMAIL)))
                .isInstanceOf(PhoneAlreadyRegisteredException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsAdminRole() {
        assertThatThrownBy(() -> service.register(command(Role.ADMIN, EMAIL)))
                .isInstanceOf(RoleNotAllowedException.class);
        verify(userRepository, never()).save(any());
    }

    private void givenSavesSucceed() {
        when(passwordHasher.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(consumerProfileRepository.save(any(ConsumerProfile.class)))
                .thenAnswer(invocation -> new ConsumerProfile(PROFILE_ID, invocation.getArgument(0)));
    }

    private static User withId(User user) {
        return new User(USER_ID, user, null, user.getFailedLoginAttempts(), null, Instant.now());
    }

    private static RegisterUserCommand command(Role role, String email) {
        return new RegisterUserCommand(role, FULL_NAME, email, PHONE, PASSWORD, null);
    }
}
