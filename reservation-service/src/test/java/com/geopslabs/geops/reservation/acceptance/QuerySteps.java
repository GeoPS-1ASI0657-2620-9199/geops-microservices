package com.geopslabs.geops.reservation.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class QuerySteps {
    private static final String RESERVATION_BY_ID = ReservationSteps.RESERVATIONS + "/%d";
    private static final String RESERVATION_BY_CODE = ReservationSteps.RESERVATIONS + "/code/%s";
    private static final String RESERVATIONS_WITH_STATUS = ReservationSteps.RESERVATIONS + "?status=%s";
    private static final Long BUSINESS_OWNER_USER_ID = 9001L;
    private static final Long UNKNOWN_RESERVATION_ID = 999_999L;

    private final HttpScenario http;
    private final ScenarioState state;

    public QuerySteps(HttpScenario http, ScenarioState state) {
        this.http = http;
        this.state = state;
    }

    @When("consumer {long} views that reservation")
    public void consumerReadsThatReservation(Long consumerId) {
        http.get(RESERVATION_BY_ID.formatted(state.reservationId()), TestIdentity.consumerToken(consumerId));
    }

    @When("consumer {long} views a reservation that does not exist")
    public void consumerReadsUnknownReservation(Long consumerId) {
        http.get(RESERVATION_BY_ID.formatted(UNKNOWN_RESERVATION_ID), TestIdentity.consumerToken(consumerId));
    }

    @When("business {long} looks up the code of that reservation")
    public void businessReadsThatCode(Long businessId) {
        http.get(RESERVATION_BY_CODE.formatted(state.reservationCode()),
                TestIdentity.businessOwnerToken(BUSINESS_OWNER_USER_ID, businessId));
    }

    @When("consumer {long} looks up the code of that reservation")
    public void consumerReadsThatCode(Long consumerId) {
        http.get(RESERVATION_BY_CODE.formatted(state.reservationCode()), TestIdentity.consumerToken(consumerId));
    }

    @When("consumer {long} lists their reservations with status {word}")
    public void consumerListsReservations(Long consumerId, String status) {
        http.get(RESERVATIONS_WITH_STATUS.formatted(status), TestIdentity.consumerToken(consumerId));
    }

    @Then("the response shows offer {long} titled {string} with status {word}")
    public void responseShowsOffer(Long offerId, String title, String status) throws JsonProcessingException {
        var body = http.json(http.last());
        assertThat(body.path("reservationId").asLong()).isEqualTo(state.reservationId());
        assertThat(body.path("offerId").asLong()).isEqualTo(offerId);
        assertThat(body.path("offerTitle").asText()).isEqualTo(title);
        assertThat(body.path("status").asText()).isEqualTo(status);
    }

    @And("the list only has {word} reservations for offer {long}")
    public void listHasOnlyOffers(String status, Long offerId) throws JsonProcessingException {
        var offers = new ArrayList<Long>();
        for (var item : http.json(http.last())) {
            assertThat(item.path("status").asText()).isEqualTo(status);
            offers.add(item.path("offerId").asLong());
        }
        assertThat(offers).containsExactly(offerId);
    }
}
