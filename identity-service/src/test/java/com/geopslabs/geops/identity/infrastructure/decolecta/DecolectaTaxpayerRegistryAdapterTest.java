package com.geopslabs.geops.identity.infrastructure.decolecta;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.geopslabs.geops.identity.domain.models.Ruc;
import com.geopslabs.geops.identity.domain.models.TaxpayerRegistryUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Decolecta is a WireMock server with the answers the real service gave on 22/11/2026; the real one is never called.
 */
class DecolectaTaxpayerRegistryAdapterTest {
    private static final WireMockServer DECOLECTA = new WireMockServer(wireMockConfig().dynamicPort());
    private static final String RUC_PATH = "/v1/sunat/ruc";
    private static final String TOKEN = "sk_test.token";
    private static final Ruc NATURAL_PERSON = new Ruc("10745351811");
    private static final Ruc UNKNOWN = new Ruc("20999999990");
    private static final Instant NOW = Instant.parse("2026-11-22T20:00:00Z");
    private static final String NATURAL_PERSON_ANSWER = """
            {"razon_social":"HUARCAYA MATIAS GILBERT ALONSO","numero_documento":"10745351811","estado":"ACTIVO",
             "condicion":"HABIDO","direccion":"-","ubigeo":"-","distrito":"","provincia":"","departamento":"",
             "es_agente_retencion":false,"es_buen_contribuyente":false,"locales_anexos":null}""";
    private static final String NOT_FOUND_ANSWER = """
            {"message":"not found"}""";
    private static final String LIMIT_ANSWER = """
            {"error":"Apikey Required / Limit Exceeded"}""";

    @BeforeAll
    static void start() {
        DECOLECTA.start();
    }

    @AfterAll
    static void stop() {
        DECOLECTA.stop();
    }

    @BeforeEach
    void reset() {
        DECOLECTA.resetAll();
    }

    @Test
    void readsANaturalPersonThatTheReducedRegistryListsWithoutDistrict() {
        DECOLECTA.stubFor(get(urlPathEqualTo(RUC_PATH)).willReturn(okJson(NATURAL_PERSON_ANSWER)));

        var taxpayer = adapter(new MovableClock(NOW)).findByRuc(NATURAL_PERSON).orElseThrow();

        assertThat(taxpayer.businessName()).isEqualTo("HUARCAYA MATIAS GILBERT ALONSO");
        assertThat(taxpayer.isActive()).isTrue();
        assertThat(taxpayer.isHabido()).isTrue();
        DECOLECTA.verify(getRequestedFor(urlPathEqualTo(RUC_PATH))
                .withQueryParam("numero", equalTo("10745351811"))
                .withHeader("Authorization", equalTo("Bearer " + TOKEN)));
    }

    @Test
    void aRucThatSunatDoesNotKnowIsEmpty() {
        DECOLECTA.stubFor(get(urlPathEqualTo(RUC_PATH))
                .willReturn(aResponse().withStatus(404).withBody(NOT_FOUND_ANSWER)));

        assertThat(adapter(new MovableClock(NOW)).findByRuc(UNKNOWN)).isEmpty();
    }

    @Test
    void aWrongTokenOrTheMonthlyLimitIsNotAnAnswerAboutTheRuc() {
        DECOLECTA.stubFor(get(urlPathEqualTo(RUC_PATH))
                .willReturn(aResponse().withStatus(401).withBody(LIMIT_ANSWER)));

        assertThatThrownBy(() -> adapter(new MovableClock(NOW)).findByRuc(NATURAL_PERSON))
                .isInstanceOf(TaxpayerRegistryUnavailableException.class);
    }

    @Test
    void aServerErrorLeavesTheLookupUnavailable() {
        DECOLECTA.stubFor(get(urlPathEqualTo(RUC_PATH)).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> adapter(new MovableClock(NOW)).findByRuc(NATURAL_PERSON))
                .isInstanceOf(TaxpayerRegistryUnavailableException.class);
    }

    @Test
    void aSlowAnswerLeavesTheLookupUnavailable() {
        DECOLECTA.stubFor(get(urlPathEqualTo(RUC_PATH))
                .willReturn(okJson(NATURAL_PERSON_ANSWER).withFixedDelay(1_500)));

        assertThatThrownBy(() -> adapter(new MovableClock(NOW)).findByRuc(NATURAL_PERSON))
                .isInstanceOf(TaxpayerRegistryUnavailableException.class);
    }

    @Test
    void theRetriesOfTheSameDayReuseTheAnswerAndTheNextDayAsksAgain() {
        DECOLECTA.stubFor(get(urlPathEqualTo(RUC_PATH)).willReturn(okJson(NATURAL_PERSON_ANSWER)));
        var clock = new MovableClock(NOW);
        var adapter = adapter(clock);

        adapter.findByRuc(NATURAL_PERSON);
        clock.advance(Duration.ofHours(23));
        adapter.findByRuc(NATURAL_PERSON);
        DECOLECTA.verify(1, getRequestedFor(urlPathEqualTo(RUC_PATH)));

        clock.advance(Duration.ofHours(2));
        adapter.findByRuc(NATURAL_PERSON);
        DECOLECTA.verify(2, getRequestedFor(urlPathEqualTo(RUC_PATH)));
    }

    @Test
    void aFailedLookupIsNotReusedAndTheNextRetryAsksAgain() {
        DECOLECTA.stubFor(get(urlPathEqualTo(RUC_PATH)).willReturn(aResponse().withStatus(503)));
        var adapter = adapter(new MovableClock(NOW));
        assertThatThrownBy(() -> adapter.findByRuc(NATURAL_PERSON))
                .isInstanceOf(TaxpayerRegistryUnavailableException.class);

        DECOLECTA.stubFor(get(urlPathEqualTo(RUC_PATH)).willReturn(okJson(NATURAL_PERSON_ANSWER)));

        assertThat(adapter.findByRuc(NATURAL_PERSON)).isPresent();
    }

    @Test
    void anOpenCircuitDoesNotCallDecolecta() {
        var circuitBreaker = CircuitBreaker.ofDefaults("decolecta-test");
        circuitBreaker.transitionToOpenState();
        var adapter = new DecolectaTaxpayerRegistryAdapter(properties(TOKEN), circuitBreaker, new MovableClock(NOW));

        assertThatThrownBy(() -> adapter.findByRuc(NATURAL_PERSON))
                .isInstanceOf(TaxpayerRegistryUnavailableException.class);
        DECOLECTA.verify(0, getRequestedFor(urlPathEqualTo(RUC_PATH)));
    }

    @Test
    void withoutATokenTheLookupIsNotReady() {
        var circuitBreaker = CircuitBreaker.ofDefaults("decolecta-test");
        var clock = new MovableClock(NOW);

        assertThat(new DecolectaTaxpayerRegistryAdapter(properties(""), circuitBreaker, clock).isLoaded()).isFalse();
        assertThat(new DecolectaTaxpayerRegistryAdapter(properties(TOKEN), circuitBreaker, clock).isLoaded()).isTrue();
    }

    private static DecolectaTaxpayerRegistryAdapter adapter(Clock clock) {
        return new DecolectaTaxpayerRegistryAdapter(properties(TOKEN), CircuitBreaker.ofDefaults("decolecta-test"),
                clock);
    }

    private static DecolectaProperties properties(String token) {
        return new DecolectaProperties(URI.create(DECOLECTA.baseUrl()), token, Duration.ofSeconds(1),
                Duration.ofHours(24));
    }

    /** A clock the test moves forward to check how long an answer is reused. */
    private static final class MovableClock extends Clock {
        private Instant now;

        private MovableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
