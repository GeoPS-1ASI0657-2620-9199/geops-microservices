package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.LogInCommand;
import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.models.Email;
import com.geopslabs.geops.identity.domain.models.GeoPoint;
import com.geopslabs.geops.identity.domain.models.InvalidCredentialsException;
import com.geopslabs.geops.identity.domain.models.IssuedToken;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.Ruc;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.PasswordHasherPort;
import com.geopslabs.geops.identity.domain.ports.TokenIssuerPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthenticationServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-03T15:00:00Z");
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(60);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int NO_FAILED_ATTEMPTS = 0;
    private static final Long USER_ID = 41L;
    private static final String EMAIL = "lucia.fernandez@ejemplo.pe";
    private static final String PASSWORD = "Ofertas#2026";
    private static final String WRONG_PASSWORD = "Otra#2026";
    private static final String PASSWORD_HASH = "$2a$10$stored";
    private static final String FILLER_HASH = "$2a$10$filler";
    private static final String TOKEN = "header.payload.signature";
    private static final Long OWNER_ID = 42L;
    private static final Long BUSINESS_ID = 7L;
    private static final String OWNER_EMAIL = "rosa.quispe@ejemplo.pe";

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private BusinessProfileRepositoryPort businessProfileRepository;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private TokenIssuerPort tokenIssuer;

    private UserAuthenticationService service;

    @BeforeEach
    void setUp() {
        when(passwordHasher.encode(anyString())).thenReturn(FILLER_HASH);
        service = new UserAuthenticationService(userRepository, businessProfileRepository, passwordHasher, tokenIssuer,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void issuesTokenWithConsumerIdForValidCredentials() {
        var user = consumer(NO_FAILED_ATTEMPTS, null);
        givenStoredUser(user);
        when(passwordHasher.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(tokenIssuer.issue(user, null)).thenReturn(new IssuedToken(TOKEN, TOKEN_LIFETIME));

        var result = service.logIn(new LogInCommand(EMAIL, PASSWORD));

        assertThat(result.token().value()).isEqualTo(TOKEN);
        assertThat(result.role()).isEqualTo(Role.CONSUMER);
        assertThat(result.consumerId()).isEqualTo(USER_ID);
        assertThat(result.businessId()).isNull();
        verify(businessProfileRepository, never()).findByUserId(any());
    }

    @Test
    void issuesTokenWithBusinessIdForBusinessOwner() {
        var owner = businessOwner();
        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(passwordHasher.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(businessProfileRepository.findByUserId(OWNER_ID)).thenReturn(Optional.of(businessProfile()));
        when(tokenIssuer.issue(owner, BUSINESS_ID)).thenReturn(new IssuedToken(TOKEN, TOKEN_LIFETIME));

        var result = service.logIn(new LogInCommand(OWNER_EMAIL, PASSWORD));

        assertThat(result.token().value()).isEqualTo(TOKEN);
        assertThat(result.role()).isEqualTo(Role.BUSINESS_OWNER);
        assertThat(result.businessId()).isEqualTo(BUSINESS_ID);
        assertThat(result.consumerId()).isNull();
    }

    @Test
    void refusesToIssueTokenForBusinessOwnerWithoutBusinessProfile() {
        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(businessOwner()));
        when(passwordHasher.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(businessProfileRepository.findByUserId(OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.logIn(new LogInCommand(OWNER_EMAIL, PASSWORD)))
                .isInstanceOf(IllegalStateException.class);
        verify(tokenIssuer, never()).issue(any(), any());
    }

    @Test
    void rejectsUnknownEmailAfterComparingWithFillerHash() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        var failure = logInExpectingFailure(PASSWORD);

        verify(passwordHasher).matches(PASSWORD, FILLER_HASH);
        assertThat(failure.getCode()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void answersWrongPasswordExactlyLikeUnknownEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        var unknownEmail = logInExpectingFailure(PASSWORD);
        givenStoredUser(consumer(NO_FAILED_ATTEMPTS, null));

        var wrongPassword = logInExpectingFailure(WRONG_PASSWORD);

        assertThat(wrongPassword.getCode()).isEqualTo(unknownEmail.getCode());
        assertThat(wrongPassword.getMessage()).isEqualTo(unknownEmail.getMessage());
    }

    @Test
    void countsWrongPasswordAsFailedAttempt() {
        var user = consumer(NO_FAILED_ATTEMPTS, null);
        givenStoredUser(user);

        logInExpectingFailure(WRONG_PASSWORD);

        verify(userRepository).save(user);
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void locksAccountForFifteenMinutesAfterFiveFailedAttempts() {
        var user = consumer(MAX_FAILED_ATTEMPTS - 1, null);
        givenStoredUser(user);

        logInExpectingFailure(WRONG_PASSWORD);

        assertThat(user.getLockedUntil()).isEqualTo(NOW.plus(LOCK_DURATION));
        assertThat(user.isLockedAt(NOW)).isTrue();
    }

    @Test
    void rejectsLockedAccountEvenWithCorrectPassword() {
        givenStoredUser(consumer(NO_FAILED_ATTEMPTS, NOW.plus(LOCK_DURATION)));

        logInExpectingFailure(PASSWORD);

        verify(tokenIssuer, never()).issue(any(), any());
    }

    @Test
    void resetsFailedAttemptsAfterSuccessfulLogin() {
        var user = consumer(MAX_FAILED_ATTEMPTS - 1, NOW.minus(LOCK_DURATION));
        givenStoredUser(user);
        when(passwordHasher.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(tokenIssuer.issue(eq(user), any())).thenReturn(new IssuedToken(TOKEN, TOKEN_LIFETIME));

        service.logIn(new LogInCommand(EMAIL, PASSWORD));

        verify(userRepository).save(user);
        assertThat(user.getFailedLoginAttempts()).isEqualTo(NO_FAILED_ATTEMPTS);
        assertThat(user.getLockedUntil()).isNull();
    }

    private InvalidCredentialsException logInExpectingFailure(String password) {
        return catchThrowableOfType(InvalidCredentialsException.class,
                () -> service.logIn(new LogInCommand(EMAIL, password)));
    }

    private void givenStoredUser(User user) {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    private static User businessOwner() {
        var data = User.register("Rosa Quispe Mamani", new Email(OWNER_EMAIL), "987654321", PASSWORD_HASH,
                Role.BUSINESS_OWNER);
        return new User(OWNER_ID, data, null, NO_FAILED_ATTEMPTS, null, NOW);
    }

    private static BusinessProfile businessProfile() {
        var data = BusinessProfile.register("Bodega Doña Rosa", "Bodega", new Ruc("10456789019"),
                "Jr. Huánuco 1250, La Victoria", new GeoPoint(-12.0681, -77.0350), "Lun-Sáb 07:00-22:00");
        return new BusinessProfile(BUSINESS_ID, OWNER_ID, data, data.getAccountStatus(), data.getVerificationStatus());
    }

    private static User consumer(int failedAttempts, Instant lockedUntil) {
        var data = User.register("Lucía Fernández Ríos", new Email(EMAIL), "987123456", PASSWORD_HASH,
                Role.CONSUMER);
        return new User(USER_ID, data, null, failedAttempts, lockedUntil, NOW);
    }
}
