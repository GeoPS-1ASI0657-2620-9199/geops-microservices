package com.geopslabs.geops.identity.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.geopslabs.geops.identity.infrastructure.persistence.BusinessProfileJpaRepository;
import com.geopslabs.geops.identity.infrastructure.persistence.ConsumerProfileJpaRepository;
import com.geopslabs.geops.identity.infrastructure.persistence.UserJpaRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonSteps {
    private final HttpScenario http;
    private final UserJpaRepository users;
    private final ConsumerProfileJpaRepository consumerProfiles;
    private final BusinessProfileJpaRepository businessProfiles;

    public CommonSteps(HttpScenario http, UserJpaRepository users, ConsumerProfileJpaRepository consumerProfiles,
                       BusinessProfileJpaRepository businessProfiles) {
        this.http = http;
        this.users = users;
        this.consumerProfiles = consumerProfiles;
        this.businessProfiles = businessProfiles;
    }

    @Before
    public void startWithEmptyAccounts() {
        consumerProfiles.deleteAll();
        businessProfiles.deleteAll();
        users.deleteAll();
    }

    @Then("the response has status {int}")
    public void theResponseHasStatus(int status) {
        assertThat(http.lastStatus()).as(http.lastBody()).isEqualTo(status);
    }

    @And("the field {string} is {string}")
    public void theFieldIs(String field, String value) throws JsonProcessingException {
        assertThat(http.field(field)).isEqualTo(value);
    }

    @And("the response does not contain {string}")
    public void theResponseDoesNotContain(String text) {
        assertThat(http.lastBody()).doesNotContain(text);
    }
}
