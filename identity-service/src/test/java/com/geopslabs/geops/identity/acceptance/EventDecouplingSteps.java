package com.geopslabs.geops.identity.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.identity.domain.models.Email;
import com.geopslabs.geops.identity.domain.models.EmailConfirmation;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** US28: what Identity leaves in its outbox for Notification, and the confirmation link that travels in it. */
public class EventDecouplingSteps {
    private static final String CONFIRMATION_PATH = "/api/v1/auth/email-confirmations";
    private static final String USER_REGISTERED = "UserRegistered";
    private static final String COUNT_EVENTS =
            "SELECT count(*) FROM outbox_events WHERE event_type = ? AND aggregate_id = ?";
    private static final String LATEST_PAYLOAD = "SELECT payload::text FROM outbox_events "
            + "WHERE event_type = ? AND aggregate_id = ? ORDER BY created_at DESC LIMIT 1";

    private final HttpScenario http;
    private final UserRepositoryPort userRepository;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final boolean relayEnabled;

    public EventDecouplingSteps(HttpScenario http, UserRepositoryPort userRepository, JdbcTemplate jdbc,
                                ObjectMapper objectMapper,
                                @Value("${geops.outbox.relay-enabled}") boolean relayEnabled) {
        this.http = http;
        this.userRepository = userRepository;
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.relayEnabled = relayEnabled;
    }

    @Given("Notification and the event bus are not reachable from Identity")
    public void nothingIsReachable() {
        // No Notification runs next to this service and the relay is off, so nothing leaves Identity.
        assertThat(relayEnabled).isFalse();
    }

    @And("the outbox has the event {string} of the account {string}")
    public void theOutboxHasTheEvent(String eventType, String email) {
        theOutboxHasEvents(1, eventType, email);
    }

    @And("the outbox has {int} event {string} of the account {string}")
    public void theOutboxHasEvents(int count, String eventType, String email) {
        assertThat(jdbc.queryForObject(COUNT_EVENTS, Long.class, eventType, userId(email))).isEqualTo(count);
    }

    @And("the event {string} carries a confirmation link that is not stored in the database")
    public void theEventCarriesTheLink(String eventType) throws JsonProcessingException {
        var payload = objectMapper.readTree(jdbc.queryForObject(
                "SELECT payload::text FROM outbox_events WHERE event_type = ?", String.class, eventType))
                .path("payload");
        var token = payload.path("confirmationToken").asText();
        assertThat(token).isNotBlank();
        assertThat(payload.path("confirmationExpiresAt").isTextual()).isTrue();
        var stored = jdbc.queryForObject("SELECT token_hash FROM email_confirmations WHERE user_id = ?",
                String.class, payload.path("userId").asLong());
        assertThat(stored).isEqualTo(EmailConfirmation.hashOf(token)).isNotEqualTo(token);
    }

    @When("they open the confirmation link of {string}")
    public void theyOpenTheLink(String email) throws JsonProcessingException {
        var envelope = objectMapper.readTree(jdbc.queryForObject(LATEST_PAYLOAD, String.class, USER_REGISTERED,
                userId(email)));
        http.post(CONFIRMATION_PATH, Map.of("token", envelope.path("payload").path("confirmationToken").asText()));
    }

    @And("the email of the account {string} is confirmed")
    public void theEmailIsConfirmed(String email) {
        assertThat(userRepository.findByEmail(new Email(email).value()).orElseThrow().getEmailConfirmedAt())
                .isNotNull();
    }

    private Long userId(String email) {
        return userRepository.findByEmail(new Email(email).value()).orElseThrow().getId();
    }
}
