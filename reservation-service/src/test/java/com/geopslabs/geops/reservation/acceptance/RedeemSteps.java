package com.geopslabs.geops.reservation.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.geopslabs.geops.reservation.domain.events.ReservationRedeemed;
import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxEventJpaRepository;
import com.geopslabs.geops.reservation.infrastructure.persistence.ReservationJpaEntity;
import com.geopslabs.geops.reservation.infrastructure.persistence.ReservationJpaRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class RedeemSteps {
    static final String OFFER_TITLE = "2x1 en almuerzos ejecutivos";
    private static final String REDEEM = ReservationSteps.RESERVATIONS + "/redeem";
    private static final String RESERVATION_BY_CODE = ReservationSteps.RESERVATIONS + "/code/%s";
    private static final String CODE_BODY = "{\"code\": \"%s\"}";
    private static final Long CONSUMER_ID = 2001L;
    private static final Long BUSINESS_OWNER_USER_ID = 9001L;
    private static final Instant RESERVED_AT = Instant.parse("2026-10-14T15:00:00Z");
    private static final int HTTP_OK = 200;

    private final HttpScenario http;
    private final MutableClock clock;
    private final ReservationJpaRepository reservations;
    private final OutboxEventJpaRepository outbox;
    private final ScenarioState state;

    public RedeemSteps(HttpScenario http, MutableClock clock, ReservationJpaRepository reservations,
                       OutboxEventJpaRepository outbox, ScenarioState state) {
        this.http = http;
        this.clock = clock;
        this.reservations = reservations;
        this.outbox = outbox;
        this.state = state;
    }

    @Before
    public void startAtTheAcceptanceTimeWithAnEmptyOutbox() {
        clock.setInstant(AcceptanceTestConfiguration.ACCEPTANCE_NOW);
        outbox.deleteAll();
    }

    @Given("business {long} has an ACTIVE reservation with code {string} for offer {long} expiring at {string}")
    public void businessHasActiveReservation(Long businessId, String code, Long offerId, String expiresAt) {
        var entity = new ReservationJpaEntity();
        entity.setCode(code);
        entity.setConsumerId(CONSUMER_ID);
        entity.setOfferId(offerId);
        entity.setBusinessId(businessId);
        entity.setOfferTitle(OFFER_TITLE);
        entity.setReservedAt(RESERVED_AT);
        entity.setExpiresAt(Instant.parse(expiresAt));
        entity.setStatus(ReservationStatus.ACTIVE);
        var saved = reservations.save(entity);
        state.rememberReservation(saved.getId(), saved.getCode());
    }

    @Given("the current time is {string}")
    public void theCurrentTimeIs(String instant) {
        clock.setInstant(Instant.parse(instant));
    }

    @When("business owner of business {long} looks up code {string}")
    public void businessOwnerLooksUp(Long businessId, String code) {
        http.get(RESERVATION_BY_CODE.formatted(code),
                TestIdentity.businessOwnerToken(BUSINESS_OWNER_USER_ID, businessId));
    }

    @When("business owner of business {long} redeems code {string}")
    public void businessOwnerRedeems(Long businessId, String code) {
        http.post(REDEEM, TestIdentity.businessOwnerToken(BUSINESS_OWNER_USER_ID, businessId),
                CODE_BODY.formatted(code));
    }

    @Given("business owner of business {long} redeemed code {string}")
    public void businessOwnerRedeemed(Long businessId, String code) {
        businessOwnerRedeems(businessId, code);
        assertThat(http.last().status()).as(http.last().body()).isEqualTo(HTTP_OK);
    }

    @When("consumer {long} redeems code {string}")
    public void consumerRedeems(Long consumerId, String code) {
        http.post(REDEEM, TestIdentity.consumerToken(consumerId), CODE_BODY.formatted(code));
    }

    @Then("the response shows offer {long}, its title and expiresAt {string}")
    public void responseShowsOfferAndDeadline(Long offerId, String expiresAt) throws JsonProcessingException {
        var body = http.json(http.last());
        assertThat(body.path("offerId").asLong()).isEqualTo(offerId);
        assertThat(body.path("offerTitle").asText()).isEqualTo(OFFER_TITLE);
        assertThat(body.path("expiresAt").asText()).isEqualTo(expiresAt);
    }

    @And("one ReservationRedeemed event for the reservation is stored in the outbox")
    public void oneEventInTheOutbox() {
        assertThat(outbox.findByAggregateIdAndEventType(state.reservationId(), ReservationRedeemed.EVENT_TYPE))
                .hasSize(1);
        assertThat(outbox.count()).isEqualTo(1);
    }

    @And("there is still one ReservationRedeemed event in the outbox")
    public void stillOneEventInTheOutbox() {
        oneEventInTheOutbox();
    }

    @And("the reservation is still ACTIVE")
    public void reservationIsStillActive() {
        var entity = reservations.findById(state.reservationId()).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(entity.getRedeemedAt()).isNull();
        assertThat(outbox.count()).isZero();
    }
}
