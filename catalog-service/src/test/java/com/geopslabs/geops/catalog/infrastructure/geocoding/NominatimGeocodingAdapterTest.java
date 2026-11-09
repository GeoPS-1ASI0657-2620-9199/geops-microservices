package com.geopslabs.geops.catalog.infrastructure.geocoding;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.GeocodingResult;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nominatim is a WireMock server; the real service is never called (E2-15). The circuit breaker has the same
 * settings as resilience4j.circuitbreaker.instances.nominatim.
 */
class NominatimGeocodingAdapterTest {
    private static final WireMockServer NOMINATIM = new WireMockServer(wireMockConfig().dynamicPort());
    private static final String ADDRESS = "Av. Larco 345, Miraflores";
    private static final String QUERY = "Av. Larco 345, Miraflores, Lima, Perú";
    private static final String SEARCH = "/search";
    private static final String LARCO = """
            [{"place_id": 1, "lat": "-12.1211", "lon": "-77.0297", "display_name": "Avenida José Larco"}]""";
    private static final String AREQUIPA = """
            [{"place_id": 2, "lat": "-16.3989", "lon": "-71.5350", "display_name": "Arequipa"}]""";
    private static final int FAILURES_TO_OPEN = 5;

    private NominatimGeocodingAdapter adapter;

    @BeforeAll
    static void startNominatim() {
        NOMINATIM.start();
    }

    @AfterAll
    static void stopNominatim() {
        NOMINATIM.stop();
    }

    @BeforeEach
    void setUp() {
        NOMINATIM.resetAll();
        adapter = adapter(Duration.ZERO);
    }

    @Test
    void returnsThePointOfAnAddressInLima() {
        NOMINATIM.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(okJson(LARCO)));

        var result = adapter.geocode(ADDRESS);

        assertThat(result).isEqualTo(GeocodingResult.found(new GeoPoint(-12.1211, -77.0297)));
        NOMINATIM.verify(getRequestedFor(urlPathEqualTo(SEARCH))
                .withQueryParam("q", equalTo(QUERY))
                .withQueryParam("format", equalTo("jsonv2"))
                .withQueryParam("limit", equalTo("1"))
                .withQueryParam("countrycodes", equalTo("pe"))
                .withHeader("User-Agent", equalTo("GeoPS/1.0 (equipo@geops.pe)")));
    }

    @Test
    void anEmptyAnswerIsNotFound() {
        NOMINATIM.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(okJson("[]")));

        assertThat(adapter.geocode(ADDRESS)).isEqualTo(GeocodingResult.notFound());
    }

    @Test
    void aPointOutsideLimaAndCallaoIsNotFound() {
        NOMINATIM.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(okJson(AREQUIPA)));

        assertThat(adapter.geocode(ADDRESS)).isEqualTo(GeocodingResult.notFound());
    }

    @Test
    void aServerErrorIsUnavailable() {
        NOMINATIM.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(aResponse().withStatus(503)));

        assertThat(adapter.geocode(ADDRESS)).isEqualTo(GeocodingResult.unavailable());
    }

    @Test
    void anAnswerSlowerThanTwoSecondsIsUnavailable() {
        NOMINATIM.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(okJson(LARCO).withFixedDelay(3_000)));
        var started = System.nanoTime();

        var result = adapter.geocode(ADDRESS);

        assertThat(result).isEqualTo(GeocodingResult.unavailable());
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofMillis(2_900));
    }

    @Test
    void afterFiveFailuresTheSixthCallDoesNotReachNominatim() {
        NOMINATIM.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(aResponse().withStatus(503)));

        for (int call = 0; call < FAILURES_TO_OPEN; call++) {
            assertThat(adapter.geocode(ADDRESS)).isEqualTo(GeocodingResult.unavailable());
        }
        var sixth = adapter.geocode(ADDRESS);

        assertThat(sixth).isEqualTo(GeocodingResult.unavailable());
        NOMINATIM.verify(FAILURES_TO_OPEN, getRequestedFor(urlPathEqualTo(SEARCH)));
    }

    @Test
    void addressesThatAreNotFoundDoNotOpenTheCircuit() {
        NOMINATIM.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(okJson("[]")));

        for (int call = 0; call <= FAILURES_TO_OPEN; call++) {
            adapter.geocode(ADDRESS);
        }

        NOMINATIM.verify(FAILURES_TO_OPEN + 1, getRequestedFor(urlPathEqualTo(SEARCH)));
    }

    @Test
    void spacesTwoRequestsByTheMinimumInterval() {
        NOMINATIM.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(okJson("[]")));
        var spaced = adapter(Duration.ofMillis(400));
        spaced.geocode(ADDRESS);
        var started = System.nanoTime();

        spaced.geocode(ADDRESS);

        assertThat(Duration.ofNanos(System.nanoTime() - started)).isGreaterThanOrEqualTo(Duration.ofMillis(300));
    }

    private static NominatimGeocodingAdapter adapter(Duration minInterval) {
        var properties = new NominatimProperties(URI.create(NOMINATIM.baseUrl()), "equipo@geops.pe",
                Duration.ofSeconds(2), minInterval);
        return new NominatimGeocodingAdapter(properties, circuitBreaker(), new NominatimRateLimiter(minInterval));
    }

    /** Same values as application.yml: five failures in ten seconds open it for 30 s. */
    private static CircuitBreaker circuitBreaker() {
        return CircuitBreaker.of("nominatim", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(100)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build());
    }
}
