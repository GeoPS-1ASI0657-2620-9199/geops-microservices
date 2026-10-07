package com.geopslabs.geops.catalog.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class OfferAvailabilitySteps {
    private static final String AVAILABILITY = "/internal/v1/offers/%d/availability";
    private static final String[] CONTRACT_FIELDS = {"offerId", "businessId", "title", "validTo", "available"};

    private final HttpScenario http;
    private final ScenarioState state;

    public OfferAvailabilitySteps(HttpScenario http, ScenarioState state) {
        this.http = http;
        this.state = state;
    }

    @When("Reservation asks for the availability of that offer")
    public void reservationAsksForThatOffer() {
        http.get(AVAILABILITY.formatted(state.currentOffer().getId()), null);
    }

    @When("Reservation asks for the availability of offer {long}")
    public void reservationAsksForOffer(Long offerId) {
        http.get(AVAILABILITY.formatted(offerId), null);
    }

    @And("the response only has the fields offerId, businessId, title, validTo and available")
    public void theResponseOnlyHasTheContractFields() throws JsonProcessingException {
        var fields = new ArrayList<String>();
        http.json(http.last()).fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder(CONTRACT_FIELDS);
    }
}
