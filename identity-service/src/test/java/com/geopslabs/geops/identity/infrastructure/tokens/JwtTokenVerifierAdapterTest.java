package com.geopslabs.geops.identity.infrastructure.tokens;

import com.geopslabs.geops.identity.acceptance.TestKeys;
import com.geopslabs.geops.identity.configuration.JwtProperties;
import com.geopslabs.geops.identity.configuration.SecurityConfiguration;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.TokenFingerprint;
import com.geopslabs.geops.identity.domain.models.User;
import com.nimbusds.jose.JOSEException;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenVerifierAdapterTest {
    private static final String ISSUER = "geops-identity";
    private static final String AUDIENCE = "geops-api";
    private static final Long USER_ID = 41L;
    private static final Long BUSINESS_ID = 7L;
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(60);

    private final JwtProperties properties =
            new JwtProperties(TestKeys.privateKeyPath(), TestKeys.publicKeyPath(), ISSUER, AUDIENCE);
    private final RsaKeyProvider keyProvider = new RsaKeyProvider(properties);
    private final JwtTokenVerifierAdapter adapter = new JwtTokenVerifierAdapter(decoder());

    @Test
    void readsWhoSentAValidTokenAndUntilWhen() {
        var token = issuerAt(Instant.now()).issue(user(USER_ID, Role.CONSUMER), null).value();

        var verified = adapter.verify(token).orElseThrow();

        assertThat(verified.userId()).isEqualTo(USER_ID);
        assertThat(verified.role()).isEqualTo(Role.CONSUMER);
        assertThat(verified.businessId()).isNull();
        assertThat(Duration.between(verified.issuedAt(), verified.expiresAt())).isEqualTo(TOKEN_LIFETIME);
        assertThat(verified.fingerprint()).isEqualTo(TokenFingerprint.of(token));
    }

    @Test
    void readsTheBusinessOfABusinessOwner() {
        var token = issuerAt(Instant.now()).issue(user(USER_ID, Role.BUSINESS_OWNER), BUSINESS_ID).value();

        var verified = adapter.verify(token).orElseThrow();

        assertThat(verified.role()).isEqualTo(Role.BUSINESS_OWNER);
        assertThat(verified.businessId()).isEqualTo(BUSINESS_ID);
    }

    @Test
    void rejectsAnExpiredToken() {
        var token = issuerAt(Instant.now().minus(Duration.ofHours(2))).issue(user(USER_ID, Role.CONSUMER), null)
                .value();

        assertThat(adapter.verify(token)).isEmpty();
    }

    @Test
    void rejectsATokenWithAnotherSignature() {
        var token = issuerAt(Instant.now()).issue(user(USER_ID, Role.CONSUMER), null).value();
        var parts = token.split("\\.");
        var otherToken = issuerAt(Instant.now()).issue(user(USER_ID + 1, Role.CONSUMER), null).value();

        assertThat(adapter.verify(parts[0] + "." + parts[1] + "." + otherToken.split("\\.")[2])).isEmpty();
    }

    @Test
    void rejectsSomethingThatIsNotAToken() {
        assertThat(adapter.verify("esto-no-es-un-token")).isEmpty();
    }

    private RsaTokenIssuerAdapter issuerAt(Instant now) {
        return new RsaTokenIssuerAdapter(keyProvider, properties, Clock.fixed(now, ZoneOffset.UTC));
    }

    private JwtDecoder decoder() {
        try {
            return new SecurityConfiguration().jwtDecoder(keyProvider, properties);
        } catch (JOSEException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static User user(Long id, Role role) {
        var data = new User("Prueba", "prueba@ejemplo.pe", "900000000", "hash", role);
        return new User(id, data, null, 0, null, Instant.now());
    }
}
