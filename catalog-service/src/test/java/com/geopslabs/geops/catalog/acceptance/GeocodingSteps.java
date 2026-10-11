package com.geopslabs.geops.catalog.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.catalog.configuration.GeocodingConfiguration;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * US07 and the public source offers of US39. Nominatim is the WireMock server of the suite.
 */
public class GeocodingSteps {
    private static final String PUBLIC_OFFERS = "/api/v1/admin/public-offers";
    private static final String RUNS = "/api/v1/admin/geocoding-runs";
    private static final String SUMMARY = "/api/v1/admin/geocoding-summary";
    private static final String PUBLIC_SOURCES = "/api/v1/offers/nearby/public-sources?lat=%s&lng=%s&radiusMinutes=%d";
    private static final String SEARCH_PATH = "/search";
    private static final long ADMIN_ID = 1L;
    private static final long OWNER_ID = 9001L;
    private static final long BUSINESS_ID = 84L;
    private static final int CREATED = 201;
    private static final int OK = 200;
    private static final int SERVICE_UNAVAILABLE = 503;
    private static final int HIGHEST_PRIORITY = 1;
    private static final int DEFAULT_PRIORITY = 5;
    private static final double SAME_COORDINATE = 1e-6;
    private static final double METERS_PER_DEGREE = Math.toRadians(1) * GeoPoint.EARTH_MEAN_RADIUS_METERS;
    private static final String DEFAULT_SOURCE = "Municipalidad de Lima";
    private static final String DEFAULT_CATEGORY = "Librería y útiles";
    private static final String CONDITIONS = "Entrada libre";
    private static final String TITLE = "title";

    private final HttpScenario http;
    private final ScenarioState state;
    private final OfferRepositoryPort offers;
    private final CircuitBreakerRegistry circuitBreakers;
    private final ObjectMapper objectMapper;
    private final MutableClock clock;
    private final Map<String, Long> offerIds = new HashMap<>();
    private JsonNode lastRun;

    public GeocodingSteps(HttpScenario http, ScenarioState state, OfferRepositoryPort offers,
                          CircuitBreakerRegistry circuitBreakers, ObjectMapper objectMapper, MutableClock clock) {
        this.http = http;
        this.state = state;
        this.offers = offers;
        this.circuitBreakers = circuitBreakers;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Before
    public void nominatimStartsWithoutAnswers() {
        CucumberSpringConfiguration.NOMINATIM.resetAll();
        circuitBreakers.circuitBreaker(GeocodingConfiguration.NOMINATIM_CIRCUIT_BREAKER).reset();
    }

    @Given("the administrator loaded these public source offers:")
    public void administratorLoaded(DataTable table) throws JsonProcessingException {
        var rows = table.asMaps();
        var batch = new ArrayList<Map<String, Object>>();
        rows.forEach(row -> batch.add(publicOffer(row.get(TITLE), row.get("address"), DEFAULT_SOURCE)));
        var ids = load(batch);
        for (var index = 0; index < rows.size(); index++) {
            offerIds.put(rows.get(index).get(TITLE), ids.get(index).asLong());
        }
    }

    @Given("a GEOCODED public source offer {string} from {string} {int} meters north of the origin")
    public void geocodedPublicOffer(String title, String source, int meters) throws JsonProcessingException {
        var origin = state.origin();
        var location = new GeoPoint(origin.latitude() + meters / METERS_PER_DEGREE, origin.longitude());
        var id = load(List.of(publicOffer(title, "Jr. de la Unión 300, Cercado de Lima", source))).get(0).asLong();
        assertThat(offers.markGeocoded(id, location)).isTrue();
    }

    @Given("Nominatim finds {string} at latitude {double} and longitude {double}")
    @Given("Nominatim now finds {string} at latitude {double} and longitude {double}")
    public void nominatimFinds(String address, double latitude, double longitude) {
        CucumberSpringConfiguration.NOMINATIM.stubFor(get(urlPathEqualTo(SEARCH_PATH))
                .withQueryParam("q", containing(address))
                .atPriority(DEFAULT_PRIORITY)
                .willReturn(okJson("[{\"lat\": \"%s\", \"lon\": \"%s\"}]".formatted(latitude, longitude))));
    }

    @Given("Nominatim finds nothing for {string}")
    public void nominatimFindsNothing(String address) {
        CucumberSpringConfiguration.NOMINATIM.stubFor(get(urlPathEqualTo(SEARCH_PATH))
                .withQueryParam("q", containing(address))
                .atPriority(DEFAULT_PRIORITY)
                .willReturn(okJson("[]")));
    }

    @Given("Nominatim answers 503")
    public void nominatimIsDown() {
        CucumberSpringConfiguration.NOMINATIM.stubFor(get(urlPathEqualTo(SEARCH_PATH))
                .atPriority(HIGHEST_PRIORITY)
                .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE)));
    }

    @When("the administrator runs the geocoding")
    @When("the administrator runs the geocoding again")
    @Given("the administrator ran the geocoding")
    public void administratorRunsTheGeocoding() throws JsonProcessingException {
        var run = http.post(RUNS, TestIdentity.adminToken(ADMIN_ID), "{}");
        assertThat(run.status()).as(run.body()).isEqualTo(OK);
        lastRun = http.json(run);
    }

    @When("a business owner runs the geocoding")
    public void businessOwnerRunsTheGeocoding() {
        http.post(RUNS, TestIdentity.businessOwnerToken(OWNER_ID, BUSINESS_ID), "{}");
    }

    @Then("{string} is GEOCODED at latitude {double} and longitude {double}")
    @Then("{string} is still at latitude {double} and longitude {double}")
    public void isGeocodedAt(String title, double latitude, double longitude) {
        var offer = offer(title);
        assertThat(offer.getGeocodingStatus()).isEqualTo(GeocodingStatus.GEOCODED);
        assertThat(offer.getLocation().latitude()).isCloseTo(latitude, within(SAME_COORDINATE));
        assertThat(offer.getLocation().longitude()).isCloseTo(longitude, within(SAME_COORDINATE));
    }

    @Then("{string} is in MANUAL_REVIEW")
    public void isInManualReview(String title) {
        assertThat(offer(title).getGeocodingStatus()).isEqualTo(GeocodingStatus.MANUAL_REVIEW);
        assertThat(offer(title).getLocation()).isNull();
    }

    @Then("both offers are still PENDING")
    public void bothOffersArePending() {
        assertThat(offerIds.keySet()).hasSize(2).allSatisfy(title ->
                assertThat(offer(title).getGeocodingStatus()).isEqualTo(GeocodingStatus.PENDING));
    }

    @Then("the run reports {int} geocoded and {int} in manual review")
    public void runReports(int geocoded, int manualReview) {
        assertThat(lastRun.path("geocoded").asInt()).isEqualTo(geocoded);
        assertThat(lastRun.path("manualReview").asInt()).isEqualTo(manualReview);
        assertThat(lastRun.path("processed").asInt()).isEqualTo(geocoded + manualReview);
    }

    @Then("the run reports {int} processed")
    public void runReportsProcessed(int processed) {
        assertThat(lastRun.path("processed").asInt()).isEqualTo(processed);
    }

    @Then("the run reports that it stopped because of the provider")
    public void runStoppedByProvider() {
        assertThat(lastRun.path("stoppedByProvider").asBoolean()).isTrue();
        assertThat(lastRun.path("stillPending").asInt()).isEqualTo(offerIds.size());
    }

    @Then("the geocoding summary shows {int} geocoded and {int} in manual review")
    public void summaryShows(int geocoded, int manualReview) throws JsonProcessingException {
        var summary = http.json(http.get(SUMMARY, TestIdentity.adminToken(ADMIN_ID)));
        assertThat(summary.path("geocoded").asInt()).isEqualTo(geocoded);
        assertThat(summary.path("manualReview").asInt()).isEqualTo(manualReview);
    }

    @Then("the public sources result has {string} from {string}")
    public void publicSourcesResultHas(String title, String source) throws JsonProcessingException {
        var origin = state.origin();
        var result = http.get(PUBLIC_SOURCES.formatted(origin.latitude(), origin.longitude(), 10), null);
        assertThat(result.status()).as(result.body()).isEqualTo(OK);
        var content = http.json(result).path("content");
        assertThat(content).anySatisfy(node -> {
            assertThat(node.path(TITLE).asText()).isEqualTo(title);
            assertThat(node.path("sourceName").asText()).isEqualTo(source);
            assertThat(node.has("verifiedSeal")).isFalse();
            assertThat(node.has("businessId")).isFalse();
        });
    }

    private JsonNode load(List<Map<String, Object>> batch) throws JsonProcessingException {
        var loaded = http.post(PUBLIC_OFFERS, TestIdentity.adminToken(ADMIN_ID),
                objectMapper.writeValueAsString(Map.of("offers", batch)));
        assertThat(loaded.status()).as(loaded.body()).isEqualTo(CREATED);
        return http.json(loaded).path("offerIds");
    }

    private Map<String, Object> publicOffer(String title, String address, String source) {
        var validTo = LocalDate.now(clock).plusDays(20).toString();
        return Map.of(TITLE, title, "conditions", CONDITIONS, "price", 0, "validTo", validTo,
                "category", DEFAULT_CATEGORY, "address", address, "sourceName", source);
    }

    private Offer offer(String title) {
        return offers.findById(offerIds.get(title)).orElseThrow();
    }
}
