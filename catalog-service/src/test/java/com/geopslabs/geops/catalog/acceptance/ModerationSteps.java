package com.geopslabs.geops.catalog.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.catalog.domain.events.OfferRemoved;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.infrastructure.persistence.OutboxEventJpaRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps of US24: the administrator lists the offers, removes one with its reason and reads the moderation history
 * through /api/v1/admin.
 */
public class ModerationSteps {
    private static final String OFFERS_PATH = "/api/v1/admin/offers";
    private static final String OFFER_PATH = "/api/v1/admin/offers/%d";
    private static final String HISTORY_PATH = "/api/v1/admin/moderation-actions";
    private static final String DETAIL_PATH = "/api/v1/offers/%d";
    private static final long OWNER_USER_ID = 9001L;
    private static final int OK = 200;

    private final ScenarioState state;
    private final HttpScenario http;
    private final MerchantStandingRepositoryPort standings;
    private final OutboxEventJpaRepository outbox;
    private final MutableClock clock;
    private final ObjectMapper objectMapper;

    public ModerationSteps(ScenarioState state, HttpScenario http, MerchantStandingRepositoryPort standings,
                           OutboxEventJpaRepository outbox, MutableClock clock, ObjectMapper objectMapper) {
        this.state = state;
        this.http = http;
        this.standings = standings;
        this.outbox = outbox;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Given("consumers opened {int} reports against business {long}")
    public void consumersOpenedReports(int openReports, Long businessId) {
        var current = standings.findByBusinessId(businessId).orElseThrow();
        standings.save(new MerchantStanding(businessId, current.getBusinessName(), current.isRucVerified(),
                openReports, current.getComplianceIndex(), current.hasVerifiedSeal(), clock.instant()));
    }

    @When("administrator {long} lists the offers for moderation")
    public void listsTheOffers(Long adminId) {
        http.get(OFFERS_PATH, TestIdentity.adminToken(adminId));
    }

    @When("administrator {long} lists the reported offers for moderation")
    public void listsTheReportedOffers(Long adminId) {
        http.get(OFFERS_PATH + "?reported=true", TestIdentity.adminToken(adminId));
    }

    @When("administrator {long} lists the removed offers for moderation")
    public void listsTheRemovedOffers(Long adminId) {
        http.get(OFFERS_PATH + "?status=REMOVED", TestIdentity.adminToken(adminId));
    }

    @When("administrator {long} removes offer {string} because {string}")
    public void removesOffer(Long adminId, String title, String reason) throws JsonProcessingException {
        remove(state.offerId(title), TestIdentity.adminToken(adminId), reason);
    }

    @When("administrator {long} removes offer {long} because {string}")
    public void removesOfferById(Long adminId, Long offerId, String reason) throws JsonProcessingException {
        remove(offerId, TestIdentity.adminToken(adminId), reason);
    }

    @Given("administrator {long} removed offer {string} because {string}")
    public void removedOffer(Long adminId, String title, String reason) throws JsonProcessingException {
        removesOffer(adminId, title, reason);
        assertThat(http.last().status()).as(http.last().body()).isEqualTo(OK);
    }

    @When("business owner of business {long} removes offer {string} because {string}")
    public void businessOwnerRemovesOffer(Long businessId, String title, String reason)
            throws JsonProcessingException {
        remove(state.offerId(title), TestIdentity.businessOwnerToken(OWNER_USER_ID, businessId), reason);
    }

    @When("business owner of business {long} asks for the moderation panel")
    public void businessOwnerAsksForThePanel(Long businessId) {
        http.get(OFFERS_PATH, TestIdentity.businessOwnerToken(OWNER_USER_ID, businessId));
    }

    @When("someone without a token asks for the moderation panel")
    public void someoneWithoutTokenAsksForThePanel() {
        http.get(OFFERS_PATH, null);
    }

    @When("a consumer opens the detail of offer {string}")
    public void consumerOpensDetail(String title) {
        http.get(DETAIL_PATH.formatted(state.offerId(title)), null);
    }

    @Then("the moderation list has offer {string} with status {string} of {string}")
    public void listHasOffer(String title, String status, String businessName) throws JsonProcessingException {
        var offer = itemsOf(http.json(http.last())).stream()
                .filter(item -> item.path("offerId").asLong() == state.offerId(title))
                .findFirst();
        assertThat(offer).as(http.last().body()).hasValueSatisfying(item -> {
            assertThat(item.path("title").asText()).isEqualTo(title);
            assertThat(item.path("status").asText()).isEqualTo(status);
            assertThat(item.path("businessName").asText()).isEqualTo(businessName);
        });
    }

    @Then("the moderation list has only {string}")
    public void listHasOnly(String title) throws JsonProcessingException {
        var titles = itemsOf(http.json(http.last())).stream().map(item -> item.path("title").asText()).toList();
        assertThat(titles).containsExactly(title);
    }

    @And("the moderation history shows that administrator {long} removed {string} because {string}")
    public void historyShows(Long adminId, String title, String reason) throws JsonProcessingException {
        var history = history();
        assertThat(history).singleElement().satisfies(entry -> {
            assertThat(entry.path("adminId").asLong()).isEqualTo(adminId);
            assertThat(entry.path("offerId").asLong()).isEqualTo(state.offerId(title));
            assertThat(entry.path("offerTitle").asText()).isEqualTo(title);
            assertThat(entry.path("reason").asText()).isEqualTo(reason);
            assertThat(entry.path("removedAt").asText()).isNotBlank();
        });
    }

    @And("the moderation history is empty")
    public void historyIsEmpty() throws JsonProcessingException {
        assertThat(history()).isEmpty();
    }

    @And("the moderation history has {int} action(s)")
    public void historyHas(int actions) throws JsonProcessingException {
        assertThat(history()).hasSize(actions);
    }

    @And("an OfferRemoved event of offer {string} is in the outbox")
    public void offerRemovedIsInTheOutbox(String title) throws JsonProcessingException {
        var offerId = state.offerId(title);
        var payloads = new ArrayList<JsonNode>();
        for (var row : outbox.findAll()) {
            if (OfferRemoved.EVENT_TYPE.equals(row.getEventType())) {
                payloads.add(objectMapper.readTree(row.getPayload()).path("payload"));
            }
        }
        assertThat(payloads).singleElement()
                .satisfies(payload -> assertThat(payload.path("offerId").asLong()).isEqualTo(offerId));
    }

    private void remove(Long offerId, String token, String reason) throws JsonProcessingException {
        http.delete(OFFER_PATH.formatted(offerId), token, objectMapper.writeValueAsString(Map.of("reason", reason)));
    }

    /** Reads the history with its own request, so the last response of the scenario stays the one under test. */
    private List<JsonNode> history() throws JsonProcessingException {
        var current = http.last();
        var response = http.get(HISTORY_PATH, TestIdentity.adminToken(1L));
        assertThat(response.status()).as(response.body()).isEqualTo(OK);
        var items = itemsOf(http.json(response));
        http.restore(current);
        return items;
    }

    private static List<JsonNode> itemsOf(JsonNode page) {
        var items = new ArrayList<JsonNode>();
        page.path("content").forEach(items::add);
        return items;
    }
}
