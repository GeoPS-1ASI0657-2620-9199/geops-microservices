package com.geopslabs.geops.catalog.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonSteps {
    private static final LocalTime MIDDAY = LocalTime.NOON;

    private final HttpScenario http;
    private final MutableClock clock;

    public CommonSteps(HttpScenario http, MutableClock clock) {
        this.http = http;
        this.clock = clock;
    }

    @Given("the current date in Lima is {string}")
    public void theCurrentDateInLimaIs(String date) {
        clock.moveTo(LocalDate.parse(date).atTime(MIDDAY).atZone(clock.getZone()).toInstant());
    }

    @Then("the response has status {int}")
    public void theResponseHasStatus(int status) {
        assertThat(http.last().status()).as(http.last().body()).isEqualTo(status);
    }

    @And("the field {string} is {string}")
    public void theFieldIs(String field, String value) throws JsonProcessingException {
        assertThat(http.json(http.last()).path(field).asText()).isEqualTo(value);
    }
}
