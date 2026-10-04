package com.geopslabs.geops.reservation.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;

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

    @Cuando("el consumidor {long} consulta esa reserva")
    public void consumerReadsThatReservation(Long consumerId) {
        http.get(RESERVATION_BY_ID.formatted(state.reservationId()), TestIdentity.consumerToken(consumerId));
    }

    @Cuando("el consumidor {long} consulta una reserva que no existe")
    public void consumerReadsUnknownReservation(Long consumerId) {
        http.get(RESERVATION_BY_ID.formatted(UNKNOWN_RESERVATION_ID), TestIdentity.consumerToken(consumerId));
    }

    @Cuando("el comercio {long} consulta el código de esa reserva")
    public void businessReadsThatCode(Long businessId) {
        http.get(RESERVATION_BY_CODE.formatted(state.reservationCode()),
                TestIdentity.businessOwnerToken(BUSINESS_OWNER_USER_ID, businessId));
    }

    @Cuando("el consumidor {long} consulta el código de esa reserva")
    public void consumerReadsThatCode(Long consumerId) {
        http.get(RESERVATION_BY_CODE.formatted(state.reservationCode()), TestIdentity.consumerToken(consumerId));
    }

    @Cuando("el consumidor {long} lista sus reservas en {word}")
    public void consumerListsReservations(Long consumerId, String status) {
        http.get(RESERVATIONS_WITH_STATUS.formatted(status), TestIdentity.consumerToken(consumerId));
    }

    @Entonces("la respuesta muestra la oferta {long} titulada {string} en {word}")
    public void responseShowsOffer(Long offerId, String title, String status) throws JsonProcessingException {
        var body = http.json(http.last());
        assertThat(body.path("reservationId").asLong()).isEqualTo(state.reservationId());
        assertThat(body.path("offerId").asLong()).isEqualTo(offerId);
        assertThat(body.path("offerTitle").asText()).isEqualTo(title);
        assertThat(body.path("status").asText()).isEqualTo(status);
    }

    @Y("la lista tiene solo reservas en {word} de la oferta {long}")
    public void listHasOnlyOffers(String status, Long offerId) throws JsonProcessingException {
        var offers = new ArrayList<Long>();
        for (var item : http.json(http.last())) {
            assertThat(item.path("status").asText()).isEqualTo(status);
            offers.add(item.path("offerId").asLong());
        }
        assertThat(offers).containsExactly(offerId);
    }
}
