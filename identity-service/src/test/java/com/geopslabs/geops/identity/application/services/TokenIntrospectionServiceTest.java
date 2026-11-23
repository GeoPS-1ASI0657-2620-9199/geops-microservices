package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.domain.models.InvalidTokenException;
import com.geopslabs.geops.identity.domain.models.RevokedToken;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.TokenFingerprint;
import com.geopslabs.geops.identity.domain.models.VerifiedToken;
import com.geopslabs.geops.identity.domain.ports.RevokedTokenRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.TokenVerifierPort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenIntrospectionServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-22T15:30:00Z");
    private static final String TOKEN = "header.payload.signature";
    private static final TokenFingerprint FINGERPRINT = TokenFingerprint.of(TOKEN);
    private static final VerifiedToken VERIFIED = new VerifiedToken(FINGERPRINT, 41L, Role.CONSUMER, null,
            Instant.parse("2026-11-22T15:00:00Z"), Instant.parse("2026-11-22T16:00:00Z"));

    private final TokenVerifierPort tokenVerifier = mock(TokenVerifierPort.class);
    private final RevokedTokenRepositoryPort revokedTokens = mock(RevokedTokenRepositoryPort.class);
    private final TokenIntrospectionService service =
            new TokenIntrospectionService(tokenVerifier, revokedTokens, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void aValidTokenThatWasNotRevokedIsActive() {
        when(tokenVerifier.verify(TOKEN)).thenReturn(Optional.of(VERIFIED));

        var introspection = service.introspect(TOKEN);

        assertThat(introspection.active()).isTrue();
        assertThat(introspection.token()).isEqualTo(VERIFIED);
    }

    @Test
    void aRevokedTokenIsInactive() {
        when(tokenVerifier.verify(TOKEN)).thenReturn(Optional.of(VERIFIED));
        when(revokedTokens.isRevoked(FINGERPRINT)).thenReturn(true);

        var introspection = service.introspect(TOKEN);

        assertThat(introspection.active()).isFalse();
        assertThat(introspection.token()).isNull();
    }

    @Test
    void aTokenTheVerifierRejectsIsInactiveWithoutLookingForRevocations() {
        when(tokenVerifier.verify(TOKEN)).thenReturn(Optional.empty());

        assertThat(service.introspect(TOKEN).active()).isFalse();
        verify(revokedTokens, never()).isRevoked(any());
    }

    @Test
    void aMissingTokenIsInactiveWithoutVerifyingIt() {
        assertThat(service.introspect(null).active()).isFalse();
        assertThat(service.introspect(" ").active()).isFalse();
        verify(tokenVerifier, never()).verify(anyString());
    }

    @Test
    void loggingOutKeepsTheFingerprintUntilTheTokenExpires() {
        when(tokenVerifier.verify(TOKEN)).thenReturn(Optional.of(VERIFIED));

        service.logOut(TOKEN);

        verify(revokedTokens).save(new RevokedToken(FINGERPRINT, 41L, VERIFIED.expiresAt(), NOW));
    }

    @Test
    void loggingOutAgainDoesNotStoreTheTokenTwice() {
        when(tokenVerifier.verify(TOKEN)).thenReturn(Optional.of(VERIFIED));
        when(revokedTokens.isRevoked(FINGERPRINT)).thenReturn(true);

        service.logOut(TOKEN);

        verify(revokedTokens, never()).save(any());
    }

    @Test
    void loggingOutWithAnInvalidTokenIsRejected() {
        when(tokenVerifier.verify(TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.logOut(TOKEN)).isInstanceOf(InvalidTokenException.class);
        verify(revokedTokens, never()).save(any());
    }

    @Test
    void thePurgeDeletesTheRevocationsOfExpiredTokens() {
        when(revokedTokens.deleteExpiredBefore(NOW)).thenReturn(3);

        assertThat(service.purgeExpired()).isEqualTo(3);
    }
}
