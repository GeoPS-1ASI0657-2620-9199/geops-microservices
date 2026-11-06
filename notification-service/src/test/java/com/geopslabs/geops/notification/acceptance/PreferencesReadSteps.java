package com.geopslabs.geops.notification.acceptance;

import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.*;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Value;
import static org.assertj.core.api.Assertions.*;

public class PreferencesReadSteps {
    private final HttpScenario http;
    private final NotificationPreferenceRepositoryPort preferences;
    private final EmailUnsubscriptionRepositoryPort unsubscriptions;
    private final String secret;
    public PreferencesReadSteps(HttpScenario http, NotificationPreferenceRepositoryPort preferences, EmailUnsubscriptionRepositoryPort unsubscriptions, @Value("${geops.notifications.unsubscribe-secret}") String secret) { this.http = http; this.preferences = preferences; this.unsubscriptions = unsubscriptions; this.secret = secret; }
    @When("consumer {long} reads notification preferences")
    public void read(Long consumerId) { http.get("/api/v1/notification-preferences", TestIdentity.consumerToken(consumerId)); }
    @When("consumer {long} confirms the signed nearby email unsubscribe link")
    public void unsubscribe(Long consumerId) {
        http.post("/api/v1/email-unsubscriptions", null, "{\"token\":\"" + UnsubscribeToken.issue(consumerId, NotificationType.NEARBY_OFFER, secret) + "\"}");
    }
    @Then("consumer {long} stops nearby emails and keeps the email channel enabled")
    public void unchanged(Long consumerId) {
        assertThat(unsubscriptions.exists(consumerId, NotificationType.NEARBY_OFFER)).isTrue();
        assertThat(preferences.findByConsumerId(consumerId).orElseThrow().getEmailEnabled()).isTrue();
    }
}
