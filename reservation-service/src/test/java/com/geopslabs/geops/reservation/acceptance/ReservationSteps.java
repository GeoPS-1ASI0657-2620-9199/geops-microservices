package com.geopslabs.geops.reservation.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.geopslabs.geops.reservation.domain.models.OfferSnapshot;
import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import com.geopslabs.geops.reservation.infrastructure.persistence.ReservationJpaRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

public class ReservationSteps {
    static final String RESERVATIONS = "/api/v1/reservations";
    private static final String OFFER_BODY = "{\"offerId\": %d}";
    private static final String EMPTY_BODY = "{}";
    private static final Long BUSINESS_OWNER_USER_ID = 9001L;
    private static final Long BUSINESS_ID = 301L;
    private static final Long CONSUMER_ID = 2001L;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_OK = 200;
    private static final int SINGLE_RESERVATION = 1;

    private final HttpScenario http;
    private final FakeOfferCatalog catalog;
    private final ReservationJpaRepository reservations;
    private final ScenarioState state;

    public ReservationSteps(HttpScenario http, FakeOfferCatalog catalog, ReservationJpaRepository reservations,
                            ScenarioState state) {
        this.http = http;
        this.catalog = catalog;
        this.reservations = reservations;
        this.state = state;
    }

    @Before
    public void startWithEmptyReservations() {
        reservations.deleteAll();
        catalog.reset();
    }

    @Given("Catalog has offer {long} from business {long} titled {string} valid until {string}")
    public void catalogHasOffer(Long offerId, Long businessId, String title, String validTo) {
        catalog.publish(new OfferSnapshot(offerId, businessId, title, LocalDate.parse(validTo)));
    }

    @Given("Catalog marks offer {long} as unavailable")
    public void catalogMarksOfferUnavailable(Long offerId) {
        catalog.markUnavailable(offerId);
    }

    @Given("Catalog does not respond")
    public void catalogIsDown() {
        catalog.goDown();
    }

    @Given("consumer {long} already reserved offer {long}")
    public void consumerAlreadyReserved(Long consumerId, Long offerId) throws JsonProcessingException {
        var result = reserve(TestIdentity.consumerToken(consumerId), offerId);
        assertThat(result.status()).as(result.body()).isEqualTo(HTTP_CREATED);
        var body = http.json(result);
        state.rememberReservation(body.path("reservationId").asLong(), body.path("code").asText());
    }

    @Given("that reservation was already redeemed")
    public void thatReservationWasRedeemed() {
        var entity = reservations.findById(state.reservationId()).orElseThrow();
        entity.setStatus(ReservationStatus.REDEEMED);
        entity.setRedeemedAt(entity.getReservedAt());
        reservations.save(entity);
    }

    @When("consumer {long} reserves offer {long}")
    public void consumerReserves(Long consumerId, Long offerId) {
        reserve(TestIdentity.consumerToken(consumerId), offerId);
    }

    @When("consumer {long} sends a reservation without offerId")
    public void consumerReservesWithoutOffer(Long consumerId) {
        http.post(RESERVATIONS, TestIdentity.consumerToken(consumerId), EMPTY_BODY);
    }

    @When("offer {long} is reserved {}")
    public void reserveWithToken(Long offerId, String tokenKind) {
        reserve(tokenOfKind(tokenKind), offerId);
    }

    @When("consumer {long} sends {int} simultaneous reservations for offer {long}")
    public void consumerReservesConcurrently(Long consumerId, int requests, Long offerId)
            throws InterruptedException, ExecutionException {
        var request = http.postLater(RESERVATIONS, TestIdentity.consumerToken(consumerId),
                OFFER_BODY.formatted(offerId));
        var start = new CountDownLatch(SINGLE_RESERVATION);
        Callable<HttpScenario.HttpResult> afterStart = () -> {
            start.await();
            return request.call();
        };
        var executor = Executors.newFixedThreadPool(requests);
        try {
            List<Future<HttpScenario.HttpResult>> futures = new ArrayList<>();
            for (int index = 0; index < requests; index++) {
                futures.add(executor.submit(afterStart));
            }
            start.countDown();
            for (var future : futures) {
                state.concurrentResults().add(future.get());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Then("the response only has the fields reservationId, code and expiresAt")
    public void responseHasOnlyCreationFields() throws JsonProcessingException {
        var fields = new HashSet<String>();
        http.json(http.last()).fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("reservationId", "code", "expiresAt");
    }

    @And("the field {string} has {int} characters")
    public void fieldHasLength(String field, int length) throws JsonProcessingException {
        assertThat(http.json(http.last()).path(field).asText()).hasSize(length);
    }

    @And("the Location header points to the created reservation")
    public void locationPointsToReservation() throws JsonProcessingException {
        var id = http.json(http.last()).path("reservationId").asLong();
        assertThat(http.last().location()).endsWith(RESERVATIONS + "/" + id);
    }

    @And("the stored reservation is {word} with the title {string} and business {long}")
    public void storedReservationHas(String status, String title, Long businessId) throws JsonProcessingException {
        var id = http.json(http.last()).path("reservationId").asLong();
        var entity = reservations.findById(id).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(ReservationStatus.valueOf(status));
        assertThat(entity.getOfferTitle()).isEqualTo(title);
        assertThat(entity.getBusinessId()).isEqualTo(businessId);
    }

    @And("the response is the same reservation as before")
    public void responseIsTheSameReservation() throws JsonProcessingException {
        var body = http.json(http.last());
        assertThat(body.path("reservationId").asLong()).isEqualTo(state.reservationId());
        assertThat(body.path("code").asText()).isEqualTo(state.reservationCode());
    }

    @And("the response is a new reservation")
    public void responseIsANewReservation() throws JsonProcessingException {
        var body = http.json(http.last());
        assertThat(body.path("reservationId").asLong()).isNotEqualTo(state.reservationId());
        assertThat(body.path("code").asText()).isNotEqualTo(state.reservationCode());
    }

    @Then("one response has status 201 and the others 200 with the same reservation")
    public void oneCreatedOthersReturnedSame() throws JsonProcessingException {
        var results = state.concurrentResults();
        var created = results.stream().filter(result -> result.status() == HTTP_CREATED).count();
        var returned = results.stream().filter(result -> result.status() == HTTP_OK).count();
        assertThat(created).as(results.toString()).isEqualTo(SINGLE_RESERVATION);
        assertThat(created + returned).as(results.toString()).isEqualTo(results.size());
        var ids = new HashSet<Long>();
        for (var result : results) {
            ids.add(http.json(result).path("reservationId").asLong());
        }
        assertThat(ids).hasSize(SINGLE_RESERVATION);
    }

    @And("consumer {long} has {int} {word} reservation(s) for offer {long}")
    public void consumerHasReservations(Long consumerId, int expected, String status, Long offerId) {
        var count = reservations.findAll().stream()
                .filter(entity -> entity.getConsumerId().equals(consumerId))
                .filter(entity -> entity.getOfferId().equals(offerId))
                .filter(entity -> entity.getStatus() == ReservationStatus.valueOf(status))
                .count();
        assertThat(count).isEqualTo(expected);
    }

    @And("no reservation was stored")
    public void noReservationStored() {
        assertThat(reservations.count()).isZero();
    }

    private HttpScenario.HttpResult reserve(String token, Long offerId) {
        return http.post(RESERVATIONS, token, OFFER_BODY.formatted(offerId));
    }

    private static String tokenOfKind(String tokenKind) {
        return switch (tokenKind) {
            case "without a token" -> null;
            case "with a business owner token" -> TestIdentity.businessOwnerToken(BUSINESS_OWNER_USER_ID, BUSINESS_ID);
            case "with a token without roles" -> TestIdentity.tokenWithoutRoles(CONSUMER_ID);
            case "with a token from another issuer" -> TestIdentity.tokenFromAnotherIssuer(CONSUMER_ID);
            case "with a token for another audience" -> TestIdentity.tokenForAnotherAudience(CONSUMER_ID);
            case "with a token signed with another key" -> TestIdentity.tokenSignedWithAnotherKey(CONSUMER_ID);
            default -> throw new IllegalArgumentException(tokenKind);
        };
    }
}
