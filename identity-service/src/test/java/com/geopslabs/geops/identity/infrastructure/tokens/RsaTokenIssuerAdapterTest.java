package com.geopslabs.geops.identity.infrastructure.tokens;

import com.geopslabs.geops.identity.acceptance.TestKeys;
import com.geopslabs.geops.identity.configuration.JwtProperties;
import com.geopslabs.geops.identity.domain.models.Email;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class RsaTokenIssuerAdapterTest {
    private static final Instant NOW = Instant.parse("2026-10-03T15:00:00Z");
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(60);
    private static final String ISSUER = "geops-identity";
    private static final String AUDIENCE = "geops-api";
    private static final Long USER_ID = 41L;
    private static final Long OWNER_ID = 42L;
    private static final Long BUSINESS_ID = 7L;

    private final JwtProperties properties =
            new JwtProperties(TestKeys.privateKeyPath(), TestKeys.publicKeyPath(), ISSUER, AUDIENCE);
    private final RsaTokenIssuerAdapter adapter =
            new RsaTokenIssuerAdapter(new RsaKeyProvider(properties), properties, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void signsWithRs256AndThePublishedKid() throws ParseException, JOSEException {
        var jwt = SignedJWT.parse(adapter.issue(consumer(), null).value());
        var publishedKey = JWKSet.parse(adapter.publicKeys()).getKeyByKeyId(jwt.getHeader().getKeyID());

        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
        assertThat(jwt.verify(new RSASSAVerifier(publishedKey.toRSAKey()))).isTrue();
    }

    @Test
    void carriesTheClaimsAgreedWithTheGateway() throws ParseException {
        var issued = adapter.issue(consumer(), null);
        var claims = SignedJWT.parse(issued.value()).getJWTClaimsSet();

        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getAudience()).containsExactly(AUDIENCE);
        assertThat(claims.getSubject()).isEqualTo(String.valueOf(USER_ID));
        assertThat(claims.getStringListClaim("roles")).containsExactly("ROLE_CONSUMER");
        assertThat(claims.getClaims()).doesNotContainKeys("businessId", "consumerId", "jti");
        assertThat(claims.getIssueTime()).isEqualTo(Date.from(NOW));
        assertThat(claims.getExpirationTime()).isEqualTo(Date.from(NOW.plus(TOKEN_LIFETIME)));
        assertThat(issued.lifetime()).isEqualTo(TOKEN_LIFETIME);
    }

    @Test
    void carriesRoleAndBusinessIdForBusinessOwner() throws ParseException {
        var claims = SignedJWT.parse(adapter.issue(businessOwner(), BUSINESS_ID).value()).getJWTClaimsSet();

        assertThat(claims.getClaims().keySet())
                .containsExactlyInAnyOrder("iss", "aud", "sub", "roles", "businessId", "iat", "exp");
        assertThat(claims.getSubject()).isEqualTo(String.valueOf(OWNER_ID));
        assertThat(claims.getStringListClaim("roles")).containsExactly("ROLE_BUSINESS_OWNER");
        assertThat(claims.getLongClaim("businessId")).isEqualTo(BUSINESS_ID);
    }

    private static User businessOwner() {
        var data = User.register("Rosa Quispe Mamani", new Email("rosa.quispe@ejemplo.pe"), "987654321",
                "$2a$10$stored", Role.BUSINESS_OWNER);
        return new User(OWNER_ID, data, null, 0, null, NOW);
    }

    private static User consumer() {
        var data = User.register("Lucía Fernández Ríos", new Email("lucia.fernandez@ejemplo.pe"), "987123456",
                "$2a$10$stored", Role.CONSUMER);
        return new User(USER_ID, data, null, 0, null, NOW);
    }
}
