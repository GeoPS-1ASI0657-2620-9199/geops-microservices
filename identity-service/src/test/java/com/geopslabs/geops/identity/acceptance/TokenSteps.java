package com.geopslabs.geops.identity.acceptance;

import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.ports.TokenIssuerPort;
import io.cucumber.java.en.When;

import java.time.Instant;
import java.util.Map;

/**
 * Tokens signed by Identity's own issuer, the same key its profile routes validate with.
 */
public class TokenSteps {
    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String BUSINESS_PROFILE_PATH = "/api/v1/business-profiles/%d";
    private static final String CONSUMER_PROFILE_PATH = "/api/v1/consumer-profiles/me";
    private static final String NOT_A_TOKEN = "eyJhbGciOiJSUzI1NiJ9.e30.c2lnbmF0dXJh";
    private static final long ANY_USER_ID = 7001L;
    private static final long ANY_BUSINESS_ID = 7002L;

    private final HttpScenario http;
    private final TokenIssuerPort tokenIssuer;

    public TokenSteps(HttpScenario http, TokenIssuerPort tokenIssuer) {
        this.http = http;
        this.tokenIssuer = tokenIssuer;
    }

    @When("an anonymous user reads the business profile {long}")
    public void anonymousReadsBusinessProfile(long businessId) {
        http.get(BUSINESS_PROFILE_PATH.formatted(businessId));
    }

    @When("a consumer token reads the business profile {long}")
    public void consumerReadsBusinessProfile(long businessId) {
        http.get(BUSINESS_PROFILE_PATH.formatted(businessId), tokenFor(Role.CONSUMER, null));
    }

    @When("a business owner token reads the consumer profile")
    public void businessOwnerReadsConsumerProfile() {
        http.get(CONSUMER_PROFILE_PATH, tokenFor(Role.BUSINESS_OWNER, ANY_BUSINESS_ID));
    }

    @When("a request with a malformed token reads the business profile {long}")
    public void malformedTokenReadsBusinessProfile(long businessId) {
        http.get(BUSINESS_PROFILE_PATH.formatted(businessId), NOT_A_TOKEN);
    }

    @When("they log in with {string} and {string} while the browser still sends an invalid token")
    public void logsInWithStaleToken(String email, String password) {
        http.post(LOGIN_PATH, Map.of("email", email, "password", password), NOT_A_TOKEN);
    }

    String tokenFor(Role role, Long businessId) {
        return tokenFor(ANY_USER_ID, role, businessId);
    }

    String tokenFor(Long userId, Role role, Long businessId) {
        var data = new User("Prueba", "prueba@ejemplo.pe", "900000000", "hash", role);
        var user = new User(userId, data, null, 0, null, Instant.now());
        return tokenIssuer.issue(user, businessId).value();
    }
}
