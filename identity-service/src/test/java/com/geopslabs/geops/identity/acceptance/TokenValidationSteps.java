package com.geopslabs.geops.identity.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.geopslabs.geops.identity.configuration.JwtProperties;
import com.geopslabs.geops.identity.domain.models.Email;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import com.geopslabs.geops.identity.infrastructure.persistence.UserJpaRepository;
import com.geopslabs.geops.identity.infrastructure.tokens.RsaKeyProvider;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US26: the token validation and the logout, with tokens signed by the key of Identity or forged to be rejected.
 */
public class TokenValidationSteps {
    private static final String INTROSPECT_PATH = "/api/v1/auth/introspect";
    private static final String LOGOUT_PATH = "/api/v1/auth/logout";
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String ROLES_CLAIM = "roles";
    private static final String CONSUMER_ROLE = "ROLE_CONSUMER";
    private static final String OTHER_AUDIENCE = "otro-sistema";
    private static final String NOT_A_TOKEN = "esto-no-es-un-token";
    private static final long ANY_USER_ID = 7001L;
    private static final int KEY_SIZE = 2048;
    private static final Duration LIFETIME = Duration.ofHours(1);
    private static final Duration EXPIRED_AGO = Duration.ofMinutes(10);

    private final HttpScenario http;
    private final TokenSteps tokens;
    private final RsaKeyProvider keyProvider;
    private final JwtProperties jwtProperties;
    private final UserRepositoryPort userRepository;
    private final UserJpaRepository users;
    private String receivedToken;

    public TokenValidationSteps(HttpScenario http, TokenSteps tokens, RsaKeyProvider keyProvider,
                                JwtProperties jwtProperties, UserRepositoryPort userRepository,
                                UserJpaRepository users) {
        this.http = http;
        this.tokens = tokens;
        this.keyProvider = keyProvider;
        this.jwtProperties = jwtProperties;
        this.userRepository = userRepository;
        this.users = users;
    }

    @When("the token they received is validated")
    public void theReceivedTokenIsValidated() throws JsonProcessingException {
        http.post(INTROSPECT_PATH, null, receivedToken());
    }

    @When("they log out with the token they received")
    public void theyLogOutWithTheReceivedToken() throws JsonProcessingException {
        http.post(LOGOUT_PATH, null, receivedToken());
    }

    @When("^a token that is (.+) is validated$")
    public void aRejectedTokenIsValidated(String kind) throws JOSEException {
        http.post(INTROSPECT_PATH, null, tokenThatIs(kind));
    }

    @When("^a token that is (.+) is used to log out$")
    public void aRejectedTokenLogsOut(String kind) throws JOSEException {
        http.post(LOGOUT_PATH, null, tokenThatIs(kind));
    }

    @When("a valid token of the account {long}, which is not stored, is validated")
    public void aTokenOfAnAccountThatIsNotStoredIsValidated(long userId) {
        assertThat(users.existsById(userId)).isFalse();
        http.post(INTROSPECT_PATH, null, tokens.tokenFor(userId, Role.CONSUMER, null));
    }

    @And("the field {string} is the id of the account {string}")
    public void theFieldIsTheAccountId(String field, String email) throws JsonProcessingException {
        var user = userRepository.findByEmail(new Email(email).value()).orElseThrow();
        assertThat(http.field(field)).isEqualTo(String.valueOf(user.getId()));
    }

    @And("the validity ends one hour after the token was issued")
    public void theValidityEndsOneHourAfterIssue() throws JsonProcessingException {
        var issuedAt = Instant.parse(http.field("issuedAt"));
        var expiresAt = Instant.parse(http.field("expiresAt"));
        assertThat(Duration.between(issuedAt, expiresAt)).isEqualTo(LIFETIME);
        assertThat(expiresAt).isAfter(Instant.now());
    }

    /** The token of the last login; kept so it can be used again after other requests. */
    private String receivedToken() throws JsonProcessingException {
        if (receivedToken == null) {
            receivedToken = http.field(ACCESS_TOKEN);
        }
        return receivedToken;
    }

    private String tokenThatIs(String kind) throws JOSEException {
        var now = Instant.now();
        return switch (kind) {
            case "expired" -> signed(keyProvider.signingKey(), jwtProperties.audience(),
                    now.minus(LIFETIME).minus(EXPIRED_AGO), now.minus(EXPIRED_AGO));
            case "altered after signing" -> altered(tokens.tokenFor(Role.CONSUMER, null));
            case "signed with another key" -> signed(new RSAKeyGenerator(KEY_SIZE)
                    .keyID(keyProvider.signingKey().getKeyID()).generate(), jwtProperties.audience(), now,
                    now.plus(LIFETIME));
            case "issued for another system" -> signed(keyProvider.signingKey(), OTHER_AUDIENCE, now,
                    now.plus(LIFETIME));
            case "malformed" -> NOT_A_TOKEN;
            case "missing" -> null;
            default -> throw new IllegalArgumentException("Unknown kind of token: " + kind);
        };
    }

    private String signed(RSAKey key, String audience, Instant issuedAt, Instant expiresAt) throws JOSEException {
        var claims = new JWTClaimsSet.Builder()
                .issuer(jwtProperties.issuer())
                .audience(audience)
                .subject(String.valueOf(ANY_USER_ID))
                .claim(ROLES_CLAIM, List.of(CONSUMER_ROLE))
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build();
        var header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).type(JOSEObjectType.JWT).build();
        var jwt = new SignedJWT(header, claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    /** Changes the subject inside the payload and keeps the original signature. */
    private static String altered(String token) {
        var parts = token.split("\\.");
        var payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        var forged = payload.replace("\"sub\":\"" + ANY_USER_ID + "\"", "\"sub\":\"1\"");
        assertThat(forged).isNotEqualTo(payload);
        var encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(forged.getBytes(StandardCharsets.UTF_8));
        return parts[0] + "." + encoded + "." + parts[2];
    }
}
