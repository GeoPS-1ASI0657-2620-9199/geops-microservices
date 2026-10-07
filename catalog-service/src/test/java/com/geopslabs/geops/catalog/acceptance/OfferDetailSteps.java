package com.geopslabs.geops.catalog.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class OfferDetailSteps {
    private static final String OFFER_DETAIL = "/api/v1/offers/%d";

    private final HttpScenario http;
    private final ScenarioState state;

    public OfferDetailSteps(HttpScenario http, ScenarioState state) {
        this.http = http;
        this.state = state;
    }

    @When("a consumer opens the detail of that offer")
    public void aConsumerOpensTheDetailOfThatOffer() {
        http.get(OFFER_DETAIL.formatted(state.currentOffer().getId()), null);
    }

    @When("a consumer opens the detail of offer {long}")
    public void aConsumerOpensTheDetailOfOffer(Long offerId) {
        http.get(OFFER_DETAIL.formatted(offerId), null);
    }

    @And("the detail shows the price, validity, conditions, address and coordinates of that offer")
    public void theDetailShowsThatOffer() throws JsonProcessingException {
        var offer = state.currentOffer();
        var body = http.json(http.last());
        assertThat(body.path("price").decimalValue()).isEqualByComparingTo(offer.getPrice().amount());
        assertThat(body.path("validTo").asText()).isEqualTo(offer.getValidTo().toString());
        assertThat(body.path("conditions").asText()).isEqualTo(offer.getConditions());
        assertThat(body.path("address").asText()).isEqualTo(offer.getAddress());
        assertThat(body.path("latitude").asDouble()).isEqualTo(offer.getLocation().latitude());
        assertThat(body.path("longitude").asDouble()).isEqualTo(offer.getLocation().longitude());
    }
}
