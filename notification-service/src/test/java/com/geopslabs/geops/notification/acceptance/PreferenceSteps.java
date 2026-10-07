package com.geopslabs.geops.notification.acceptance;

import com.geopslabs.geops.notification.infrastructure.persistence.NotificationPreferenceJpaRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class PreferenceSteps {
    static final String PREFERENCES = "/api/v1/notification-preferences";
    private static final String PREFERENCES_BODY =
            "{\"pushEnabled\": %s, \"emailEnabled\": %s, \"dailyLimit\": %d}";
    private static final String WITHOUT_PUSH_BODY = "{\"emailEnabled\": true, \"dailyLimit\": 3}";
    private static final Long BUSINESS_OWNER_USER_ID = 9001L;
    private static final Long BUSINESS_ID = 1L;
    private static final Long CONSUMER_ID = 1L;
    private static final int HTTP_OK = 200;
    private static final int ANY_LIMIT = 3;

    private final HttpScenario http;
    private final NotificationPreferenceJpaRepository preferences;

    public PreferenceSteps(HttpScenario http, NotificationPreferenceJpaRepository preferences) {
        this.http = http;
        this.preferences = preferences;
    }

    @Given("consumer {long} set push to {word}, email to {word} and a daily limit of {int}")
    public void consumerSetPreferences(Long consumerId, String push, String email, int dailyLimit) {
        var result = send(TestIdentity.consumerToken(consumerId), push, email, dailyLimit);
        assertThat(result.status()).as(result.body()).isEqualTo(HTTP_OK);
    }

    @When("consumer {long} sets push to {word}, email to {word} and a daily limit of {int}")
    public void consumerSetsPreferences(Long consumerId, String push, String email, int dailyLimit) {
        send(TestIdentity.consumerToken(consumerId), push, email, dailyLimit);
    }

    @When("consumer {long} sends preferences without pushEnabled")
    public void consumerSendsPreferencesWithoutPush(Long consumerId) {
        http.put(PREFERENCES, TestIdentity.consumerToken(consumerId), WITHOUT_PUSH_BODY);
    }

    @When("the preferences are sent {}")
    public void preferencesAreSentWithToken(String tokenKind) {
        send(tokenOfKind(tokenKind), Boolean.TRUE.toString(), Boolean.TRUE.toString(), ANY_LIMIT);
    }

    @And("the stored preference of consumer {long} has push {word}, email {word} and daily limit {int}")
    public void storedPreferenceHas(Long consumerId, String push, String email, int dailyLimit) {
        var stored = preferences.findById(consumerId).orElseThrow();
        assertThat(stored.getPushEnabled()).isEqualTo(Boolean.valueOf(push));
        assertThat(stored.getEmailEnabled()).isEqualTo(Boolean.valueOf(email));
        assertThat(stored.getDailyLimit()).isEqualTo(dailyLimit);
    }

    @And("consumer {long} has no stored preference")
    public void consumerHasNoStoredPreference(Long consumerId) {
        assertThat(preferences.findById(consumerId)).isEmpty();
    }

    private HttpScenario.HttpResult send(String token, String push, String email, int dailyLimit) {
        return http.put(PREFERENCES, token, PREFERENCES_BODY.formatted(push, email, dailyLimit));
    }

    private static String tokenOfKind(String tokenKind) {
        return switch (tokenKind) {
            case "without a token" -> null;
            case "with a business owner token" -> TestIdentity.businessOwnerToken(BUSINESS_OWNER_USER_ID, BUSINESS_ID);
            case "with a token without roles" -> TestIdentity.tokenWithoutRoles(CONSUMER_ID);
            case "with a token from another issuer" -> TestIdentity.tokenFromAnotherIssuer(CONSUMER_ID);
            case "with a token for another audience" -> TestIdentity.tokenForAnotherAudience(CONSUMER_ID);
            case "with a token signed with another key" -> TestIdentity.tokenSignedWithAnotherKey(CONSUMER_ID);
            default -> throw new IllegalArgumentException(tokenKind);
        };
    }
}
