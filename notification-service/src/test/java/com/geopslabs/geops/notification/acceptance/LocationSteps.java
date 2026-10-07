package com.geopslabs.geops.notification.acceptance;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class LocationSteps {
    private static final String LAST_LOCATION = "/api/v1/locations/last";
    private static final String LOCATION_BODY =
            "{\"latitude\": %s, \"longitude\": %s, \"accuracyMeters\": %d, \"capturedAt\": \"%s\"}";
    private static final String DEFAULT_LATITUDE = "-12.1211";
    private static final String DEFAULT_LONGITUDE = "-77.0297";
    private static final int DEFAULT_ACCURACY = 25;
    private static final int HTTP_OK = 200;
    private static final double COORDINATE_TOLERANCE = 0.000001;
    private static final String STORED_POSITION = """
            SELECT ST_SRID(position::geometry) AS srid, ST_Y(position::geometry) AS latitude,
                   ST_X(position::geometry) AS longitude, GeometryType(position::geometry) AS shape
            FROM last_known_locations WHERE consumer_id = ?""";
    private static final String COUNT_LOCATIONS = "SELECT count(*) FROM last_known_locations WHERE consumer_id = ?";
    private static final String POINT = "POINT";

    private final HttpScenario http;
    private final JdbcTemplate jdbcTemplate;

    public LocationSteps(HttpScenario http, JdbcTemplate jdbcTemplate) {
        this.http = http;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Given("consumer {long} sent a location captured at {string}")
    public void consumerSentLocation(Long consumerId, String capturedAt) {
        var result = send(consumerId, DEFAULT_LATITUDE, DEFAULT_LONGITUDE, DEFAULT_ACCURACY, capturedAt);
        assertThat(result.status()).as(result.body()).isEqualTo(HTTP_OK);
    }

    @When("consumer {long} sends latitude {string}, longitude {string}, accuracy {int} captured at {string}")
    public void consumerSendsLocation(Long consumerId, String latitude, String longitude, int accuracy,
                                      String capturedAt) {
        send(consumerId, latitude, longitude, accuracy, capturedAt);
    }

    @Then("the stored position of consumer {long} is a point with SRID {int} at latitude {double} and longitude {double}")
    public void storedPositionIs(Long consumerId, int srid, double latitude, double longitude) {
        var row = jdbcTemplate.queryForMap(STORED_POSITION, consumerId);
        assertThat(((Number) row.get("srid")).intValue()).isEqualTo(srid);
        assertThat(row.get("shape").toString().toUpperCase(Locale.ROOT)).isEqualTo(POINT);
        assertThat(((Number) row.get("latitude")).doubleValue()).isCloseTo(latitude, within(COORDINATE_TOLERANCE));
        assertThat(((Number) row.get("longitude")).doubleValue()).isCloseTo(longitude, within(COORDINATE_TOLERANCE));
    }

    @And("consumer {long} has no stored location")
    public void consumerHasNoStoredLocation(Long consumerId) {
        var count = jdbcTemplate.queryForObject(COUNT_LOCATIONS, Long.class, consumerId);
        assertThat(count).isZero();
    }

    private HttpScenario.HttpResult send(Long consumerId, String latitude, String longitude, int accuracy,
                                         String capturedAt) {
        return http.put(LAST_LOCATION, TestIdentity.consumerToken(consumerId),
                LOCATION_BODY.formatted(latitude, longitude, accuracy, capturedAt));
    }
}
