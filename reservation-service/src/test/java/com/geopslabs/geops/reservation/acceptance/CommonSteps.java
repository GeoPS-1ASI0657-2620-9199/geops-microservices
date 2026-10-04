package com.geopslabs.geops.reservation.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonSteps {
    private final HttpScenario http;

    public CommonSteps(HttpScenario http) {
        this.http = http;
    }

    @Entonces("la respuesta tiene código {int}")
    public void theResponseHasStatus(int status) {
        assertThat(http.last().status()).as(http.last().body()).isEqualTo(status);
    }

    @Y("el campo {string} es {string}")
    public void theFieldIs(String field, String value) throws JsonProcessingException {
        assertThat(http.json(http.last()).path(field).asText()).isEqualTo(value);
    }
}
