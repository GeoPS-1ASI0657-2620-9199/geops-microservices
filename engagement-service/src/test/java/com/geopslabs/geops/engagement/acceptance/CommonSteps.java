package com.geopslabs.geops.engagement.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonSteps {
    private final HttpScenario http;

    public CommonSteps(HttpScenario http) {
        this.http = http;
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
