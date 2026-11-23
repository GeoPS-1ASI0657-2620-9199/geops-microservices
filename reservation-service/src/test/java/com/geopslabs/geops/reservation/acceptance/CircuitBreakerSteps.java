package com.geopslabs.geops.reservation.acceptance;

import com.geopslabs.geops.reservation.configuration.CatalogClientConfiguration;
import com.geopslabs.geops.reservation.infrastructure.catalog.CatalogOfferCatalogAdapter;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US34: the reservation asks the stub Catalog through the real client and its circuit breaker "catalog".
 */
public class CircuitBreakerSteps {
    private static final String OFFER_BODY = "{\"offerId\": %d}";
    private static final int HTTP_OK = 200;
    private static final int HTTP_NOT_FOUND = 404;
    private static final String AVAILABLE_OFFER = """
            {"offerId": %d, "businessId": 301, "title": "Menú ejecutivo a mitad de precio",
             "validTo": "2026-10-14", "available": true}""";
    private static final String NOT_FOUND_BODY = """
            {"code": "OFFER_NOT_FOUND", "message": "Offer %d was not found"}""";

    private final HttpScenario http;
    private final FakeOfferCatalog catalog;
    private final CatalogOfferCatalogAdapter realCatalog;
    private final CircuitBreaker circuitBreaker;
    private final List<HttpScenario.HttpResult> attempts = new ArrayList<>();
    private Duration lastElapsed = Duration.ZERO;

    public CircuitBreakerSteps(HttpScenario http, FakeOfferCatalog catalog, CatalogOfferCatalogAdapter realCatalog,
                               CircuitBreakerRegistry circuitBreakers) {
        this.http = http;
        this.catalog = catalog;
        this.realCatalog = realCatalog;
        this.circuitBreaker = circuitBreakers.circuitBreaker(CatalogClientConfiguration.CATALOG);
    }

    @Before
    public void closeTheCircuit() {
        circuitBreaker.reset();
        StubCatalogServer.fail();
    }

    @Given("Catalog is reached through its real client and answers with errors")
    public void catalogAnswersWithErrors() {
        StubCatalogServer.fail();
        catalog.useRealClient(realCatalog);
    }

    @Given("Catalog answers that offer {long} does not exist")
    public void catalogAnswersNotFound(Long offerId) {
        StubCatalogServer.answer(HTTP_NOT_FOUND, NOT_FOUND_BODY.formatted(offerId));
    }

    @Given("Catalog answers again that offer {long} is available")
    public void catalogAnswersAvailable(Long offerId) {
        StubCatalogServer.answer(HTTP_OK, AVAILABLE_OFFER.formatted(offerId));
    }

    @Given("the open wait of the Catalog circuit has passed")
    public void theOpenWaitHasPassed() {
        circuitBreaker.transitionToHalfOpenState();
    }

    @When("consumer {long} tries to reserve offer {long} {int} times")
    public void consumerTriesToReserve(Long consumerId, Long offerId, int times) {
        for (int attempt = 0; attempt < times; attempt++) {
            attempts.add(http.post(ReservationSteps.RESERVATIONS, TestIdentity.consumerToken(consumerId),
                    OFFER_BODY.formatted(offerId)));
        }
    }

    @When("consumer {long} reserves offer {long} once more")
    public void consumerReservesOnceMore(Long consumerId, Long offerId) {
        var started = System.nanoTime();
        http.post(ReservationSteps.RESERVATIONS, TestIdentity.consumerToken(consumerId),
                OFFER_BODY.formatted(offerId));
        lastElapsed = Duration.ofNanos(System.nanoTime() - started);
    }

    @Then("every attempt has status {int} with the code {string}")
    public void everyAttemptHas(int status, String code) {
        assertThat(attempts).isNotEmpty().allSatisfy(result -> {
            assertThat(result.status()).as(result.body()).isEqualTo(status);
            assertThat(http.json(result).path("code").asText()).isEqualTo(code);
        });
    }

    @And("the message tells the consumer in Spanish to try again later")
    public void messageInSpanish() throws Exception {
        assertThat(http.json(http.last()).path("message").asText())
                .isEqualTo("El catálogo no responde en este momento. Intenta reservar de nuevo en unos minutos.");
    }

    @Then("the Catalog circuit is {word}")
    public void theCircuitIs(String state) {
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.valueOf(state));
    }

    @And("the answer took less than {int} milliseconds")
    public void answerTookLessThan(int milliseconds) {
        assertThat(lastElapsed).isLessThan(Duration.ofMillis(milliseconds));
    }

    @And("Catalog received {int} requests")
    public void catalogReceived(int requests) {
        assertThat(StubCatalogServer.requests()).isEqualTo(requests);
    }
}
