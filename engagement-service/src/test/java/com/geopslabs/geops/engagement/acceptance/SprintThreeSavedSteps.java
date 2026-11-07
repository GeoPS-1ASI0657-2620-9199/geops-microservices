package com.geopslabs.geops.engagement.acceptance;

import com.geopslabs.geops.engagement.application.usecases.ProjectCatalogEventsUseCase;
import com.geopslabs.geops.engagement.domain.models.*;
import io.cucumber.java.en.*;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

public class SprintThreeSavedSteps {
    private final HttpScenario http;
    private final ProjectCatalogEventsUseCase projector;
    public SprintThreeSavedSteps(HttpScenario http, ProjectCatalogEventsUseCase projector) { this.http = http; this.projector = projector; }
    @Given("Catalog published offer {long} of business {long}")
    public void published(Long offerId, Long businessId) {
        projector.offerPublished(new BusinessSnapshot(businessId, "Don Pepe"), new OfferSnapshot(offerId, businessId, "2x1", LocalDate.parse("2026-11-15"), "PUBLISHED"), Instant.parse("2026-10-08T12:00:00Z"));
    }
    @When("Catalog pauses saved offer {long}")
    public void pause(Long offerId) { projector.campaignPaused(List.of(offerId), Instant.parse("2026-10-09T12:00:00Z")); }
    @When("consumer {long} saves business {long}")
    public void save(Long consumerId, Long businessId) { http.post("/api/v1/saved-businesses", TestIdentity.consumerToken(consumerId), "{\"businessId\":" + businessId + "}"); }
    @When("consumer {long} lists saved businesses")
    public void list(Long consumerId) { http.get("/api/v1/saved-businesses", TestIdentity.consumerToken(consumerId)); }
    @Then("the saved business is called {string}")
    public void shown(String name) throws Exception { assertThat(http.json(http.last()).get(0).path("businessName").asText()).isEqualTo(name); }
    @Then("the saved offer response has status {string}")
    public void status(String status) throws Exception { assertThat(http.json(http.last()).get(0).path("status").asText()).isEqualTo(status); }
}
