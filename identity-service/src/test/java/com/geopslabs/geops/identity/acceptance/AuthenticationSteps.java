package com.geopslabs.geops.identity.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.models.ConsumerProfile;
import com.geopslabs.geops.identity.domain.models.Email;
import com.geopslabs.geops.identity.domain.models.GeoPoint;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.Ruc;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.ConsumerProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.PasswordHasherPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

import java.text.ParseException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthenticationSteps {
    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String CONSUMER_NAME = "Lucía Fernández Ríos";
    private static final String CONSUMER_PHONE = "987123456";
    private static final String OWNER_NAME = "Rosa Quispe Mamani";
    private static final String OWNER_PHONE = "987654321";
    private static final Ruc BUSINESS_RUC = new Ruc("10456789019");
    private static final String BUSINESS_ADDRESS = "Jr. Huánuco 1250, La Victoria";
    private static final GeoPoint BUSINESS_LOCATION = new GeoPoint(-12.0681, -77.0350);
    private static final String CLAIM_SEPARATOR = ",\\s*";
    private static final Duration TOKEN_LIFETIME = Duration.ofHours(1);

    private final HttpScenario http;
    private final UserRepositoryPort userRepository;
    private final ConsumerProfileRepositoryPort consumerProfileRepository;
    private final BusinessProfileRepositoryPort businessProfileRepository;
    private final PasswordHasherPort passwordHasher;
    private final Map<String, Long> businessIds = new HashMap<>();

    public AuthenticationSteps(HttpScenario http, UserRepositoryPort userRepository,
                               ConsumerProfileRepositoryPort consumerProfileRepository,
                               BusinessProfileRepositoryPort businessProfileRepository,
                               PasswordHasherPort passwordHasher) {
        this.http = http;
        this.userRepository = userRepository;
        this.consumerProfileRepository = consumerProfileRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.passwordHasher = passwordHasher;
    }

    @Given("a consumer {string} exists with password {string}")
    public void aConsumerExists(String email, String password) {
        var user = userRepository.save(User.register(CONSUMER_NAME, new Email(email), CONSUMER_PHONE,
                passwordHasher.encode(password), Role.CONSUMER));
        consumerProfileRepository.save(ConsumerProfile.createFor(user.getId()));
    }

    @Given("the business {string} owned by {string} exists with password {string}")
    public void aBusinessExists(String businessName, String email, String password) {
        var owner = userRepository.save(User.register(OWNER_NAME, new Email(email), OWNER_PHONE,
                passwordHasher.encode(password), Role.BUSINESS_OWNER));
        var profile = BusinessProfile.register(businessName, null, BUSINESS_RUC, BUSINESS_ADDRESS,
                BUSINESS_LOCATION, null);
        businessIds.put(businessName, businessProfileRepository.save(profile.ownedBy(owner.getId())).getId());
    }

    @When("they log in with {string} and {string}")
    public void logsIn(String email, String password) {
        http.post(LOGIN_PATH, Map.of("email", email, "password", password));
    }

    @And("the token signature is validated with the key published at {string}")
    public void theTokenSignatureIsValidatedWithThePublishedKey(String jwksPath)
            throws JsonProcessingException, ParseException, JOSEException {
        var token = SignedJWT.parse(http.field(ACCESS_TOKEN));
        http.get(jwksPath);
        var publishedKey = JWKSet.parse(http.lastBody()).getKeyByKeyId(token.getHeader().getKeyID());

        assertThat(token.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
        assertThat(publishedKey).isNotNull();
        assertThat(token.verify(new RSASSAVerifier(publishedKey.toRSAKey()))).isTrue();
    }

    @And("the field {string} is the id of the business {string}")
    public void theFieldIsTheBusinessId(String field, String businessName) throws JsonProcessingException {
        assertThat(http.field(field)).isEqualTo(String.valueOf(businessIds.get(businessName)));
    }

    @And("the token carries the claim {string} with the id of the business {string}")
    public void theTokenCarriesTheBusinessId(String claim, String businessName)
            throws JsonProcessingException, ParseException {
        assertThat(tokenClaims().getLongClaim(claim)).isEqualTo(businessIds.get(businessName));
    }

    @And("the token carries the claim {string} with the id of the account {string}")
    public void theTokenCarriesTheUserId(String claim, String email) throws JsonProcessingException, ParseException {
        var user = userRepository.findByEmail(new Email(email).value()).orElseThrow();
        assertThat(tokenClaims().getStringClaim(claim)).isEqualTo(String.valueOf(user.getId()));
    }

    @And("the token carries the claim {string} with {string}")
    public void theTokenCarriesTheClaim(String claim, String value) throws JsonProcessingException, ParseException {
        var actual = tokenClaims().getClaim(claim);
        if (actual instanceof List<?> values) {
            assertThat(List.<Object>copyOf(values)).containsExactly(value);
            return;
        }
        assertThat(actual).isEqualTo(value);
    }

    @And("the token carries only the claims {string}")
    public void theTokenCarriesOnlyTheClaims(String claims) throws JsonProcessingException, ParseException {
        assertThat(tokenClaims().getClaims().keySet()).containsExactlyInAnyOrderElementsOf(namesIn(claims));
    }

    @And("the token does not carry the claims {string}")
    public void theTokenDoesNotCarryTheClaims(String claims) throws JsonProcessingException, ParseException {
        assertThat(tokenClaims().getClaims().keySet()).doesNotContainAnyElementsOf(namesIn(claims));
    }

    @And("the token expires one hour after it is issued")
    public void theTokenExpiresOneHourAfterIssue() throws JsonProcessingException, ParseException {
        var claims = tokenClaims();
        var lifetime = Duration.between(claims.getIssueTime().toInstant(), claims.getExpirationTime().toInstant());
        assertThat(lifetime).isEqualTo(TOKEN_LIFETIME);
    }

    private JWTClaimsSet tokenClaims() throws JsonProcessingException, ParseException {
        return SignedJWT.parse(http.field(ACCESS_TOKEN)).getJWTClaimsSet();
    }

    private static List<String> namesIn(String claims) {
        return Arrays.asList(claims.split(CLAIM_SEPARATOR));
    }
}
