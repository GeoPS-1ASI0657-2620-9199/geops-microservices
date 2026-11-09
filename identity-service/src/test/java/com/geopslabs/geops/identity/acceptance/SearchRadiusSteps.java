package com.geopslabs.geops.identity.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.identity.domain.events.ConsumerPreferencesChanged;
import com.geopslabs.geops.identity.domain.models.ConsumerProfile;
import com.geopslabs.geops.identity.domain.models.Email;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.ports.ConsumerProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import com.geopslabs.geops.identity.infrastructure.persistence.OutboxEventJpaRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchRadiusSteps {
    private static final String RADIUS_PATH = "/api/v1/consumer-profiles/me/search-radius";
    private static final String PROFILE_PATH = "/api/v1/consumer-profiles/me";

    private final HttpScenario http;
    private final TokenSteps tokens;
    private final UserRepositoryPort users;
    private final ConsumerProfileRepositoryPort consumerProfiles;
    private final OutboxEventJpaRepository outbox;
    private final ObjectMapper objectMapper;
    private Long consumerId;

    public SearchRadiusSteps(HttpScenario http, TokenSteps tokens, UserRepositoryPort users,
                             ConsumerProfileRepositoryPort consumerProfiles, OutboxEventJpaRepository outbox,
                             ObjectMapper objectMapper) {
        this.http = http;
        this.tokens = tokens;
        this.users = users;
        this.consumerProfiles = consumerProfiles;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Given("a consumer with a session")
    public void aConsumerWithASession() {
        var user = users.save(User.register("Lucía Fernández Ríos", new Email("lucia.radio@ejemplo.pe"),
                "987123450", "hash", Role.CONSUMER));
        consumerProfiles.save(ConsumerProfile.createFor(user.getId()));
        consumerId = user.getId();
    }

    @When("the consumer saves a radius of {int} minutes")
    public void theConsumerSavesARadius(int minutes) {
        http.put(RADIUS_PATH, Map.of("radiusMinutes", minutes), tokens.tokenFor(consumerId, Role.CONSUMER, null));
    }

    @When("the consumer reads the profile")
    public void theConsumerReadsTheProfile() {
        http.get(PROFILE_PATH, tokens.tokenFor(consumerId, Role.CONSUMER, null));
    }

    @When("a business owner saves a radius of {int} minutes")
    public void aBusinessOwnerSavesARadius(int minutes) {
        http.put(RADIUS_PATH, Map.of("radiusMinutes", minutes), tokens.tokenFor(Role.BUSINESS_OWNER, 7002L));
    }

    @Then("one ConsumerPreferencesChanged event with radius {int} is stored in the outbox")
    public void oneEventIsStored(int minutes) throws JsonProcessingException {
        var events = outbox.findByAggregateIdAndEventType(consumerId, ConsumerPreferencesChanged.EVENT_TYPE);
        assertThat(events).hasSize(1);
        var payload = objectMapper.readTree(events.get(0).getPayload()).path("payload");
        assertThat(payload.path("consumerId").asLong()).isEqualTo(consumerId);
        assertThat(payload.path("radiusMinutes").asInt()).isEqualTo(minutes);
    }

    @Then("the consumer keeps a radius of {int} minutes")
    public void theConsumerKeepsTheRadius(int minutes) {
        assertThat(consumerProfiles.findByUserId(consumerId).orElseThrow().getSearchRadiusMinutes())
                .isEqualTo(minutes);
    }
}
