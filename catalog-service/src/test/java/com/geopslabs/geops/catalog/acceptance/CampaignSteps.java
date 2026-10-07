package com.geopslabs.geops.catalog.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

public class CampaignSteps {
    private static final String CAMPAIGNS = "/api/v1/campaigns";
    private static final String OFFER_DETAIL = "/api/v1/offers/%d";
    private static final String NEARBY = "/api/v1/offers/nearby?lat=%s&lng=%s&radiusMinutes=%d";
    private static final Long BUSINESS_OWNER_USER_ID = 9001L;
    private static final Long CONSUMER_ID = 2001L;
    private static final int DEFAULT_RADIUS_METERS = 800;
    private static final String DEFAULT_NAME = "Campaña de prueba";
    private static final String DEFAULT_OFFER = "Oferta de prueba";
    private static final String DEFAULT_START = "2026-10-05";
    private static final String DEFAULT_END = "2026-10-31";
    private static final String DEFAULT_VALID_TO = "2026-10-15";
    private static final double METERS_PER_DEGREE = Math.toRadians(1) * GeoPoint.EARTH_MEAN_RADIUS_METERS;
    private static final String NORTH = "north";
    private static final String SOUTH = "south";
    private static final String EAST = "east";
    private static final int CREATED = 201;
    private static final int OK = 200;
    private static final int DISTANCE_TOLERANCE_METERS = 20;
    private static final String WITHOUT_TOKEN = "without a token";
    private static final String CONSUMER_TOKEN = "with a consumer token";
    private static final String TITLE = "title";
    private static final String OFFER_ID = "offerId";

    private final HttpScenario http;
    private final ObjectMapper objectMapper;
    private final CampaignRepositoryPort campaigns;
    private String token;
    private String businessName;
    private String storeAddress;
    private double storeLatitude;
    private double storeLongitude;

    public CampaignSteps(HttpScenario http, ObjectMapper objectMapper, CampaignRepositoryPort campaigns) {
        this.http = http;
        this.objectMapper = objectMapper;
        this.campaigns = campaigns;
    }

    @Given("the business owner of business {long} {string} is authenticated")
    public void theBusinessOwnerIsAuthenticated(Long businessId, String name) {
        token = TestIdentity.businessOwnerToken(BUSINESS_OWNER_USER_ID, businessId);
        businessName = name;
    }

    @And("the store of business {long} is at {string}, latitude {double} and longitude {double}")
    public void theStoreIsAt(Long businessId, String address, double latitude, double longitude) {
        storeAddress = address;
        storeLatitude = latitude;
        storeLongitude = longitude;
    }

    @When("the business owner creates the campaign {string} from {string} to {string} with a radius zone of {int} meters and the offer {string} valid until {string}")
    public void theBusinessOwnerCreatesTheCampaign(String name, String start, String end, int radiusMeters,
                                                   String offerTitle, String validTo) throws JsonProcessingException {
        http.post(CAMPAIGNS, token, campaignBody(name, start, end, radiusZone(radiusMeters), offerTitle, validTo));
    }

    @When("^the campaign \"([^\"]*)\" is sent (without a token|with a consumer token|with a business token without its id)$")
    public void theCampaignIsSent(String name, String credentials) throws JsonProcessingException {
        var body = campaignBody(name, DEFAULT_START, DEFAULT_END, radiusZone(DEFAULT_RADIUS_METERS), DEFAULT_OFFER,
                DEFAULT_VALID_TO);
        http.post(CAMPAIGNS, tokenFor(credentials), body);
    }

    @Given("the business owner published the offer {string} in a campaign with a radius zone of {int} meters")
    public void theBusinessOwnerPublishedWithARadiusZone(String offerTitle, int radiusMeters)
            throws JsonProcessingException {
        publish(offerTitle, radiusZone(radiusMeters));
    }

    @Given("the business owner published the offer {string} in a campaign with the district zone {string} centered {int} meters {word}")
    public void theBusinessOwnerPublishedWithADistrictZone(String offerTitle, String district, int meters,
                                                         String direction) throws JsonProcessingException {
        var center = point(storeOffset(meters, direction));
        publish(offerTitle, Map.of("type", "DISTRICT", "district", district, "center", center));
    }

    @When("the business owner creates a campaign with a radius zone without radiusMeters")
    public void theBusinessOwnerCreatesACampaignWithoutRadius() throws JsonProcessingException {
        var zone = Map.of("type", "RADIUS", "center", point(store()));
        http.post(CAMPAIGNS, token, campaignBody(DEFAULT_NAME, DEFAULT_START, DEFAULT_END, zone, DEFAULT_OFFER,
                DEFAULT_VALID_TO));
    }

    @And("the offer {string} is available in its public detail")
    public void theOfferIsAvailableInItsPublicDetail(String title) throws JsonProcessingException {
        var offerId = offerIdIn(http.json(http.last()), title);
        var detail = http.json(http.get(OFFER_DETAIL.formatted(offerId), null));
        assertThat(detail.path("available").asBoolean()).isTrue();
    }

    @And("a consumer searches for offers {int} meters {word} of the store within {int} walking minutes")
    public void aConsumerSearchesAroundTheStore(int meters, String direction, int minutes) {
        var consumer = storeOffset(meters, direction);
        http.get(NEARBY.formatted(consumer.latitude(), consumer.longitude(), minutes), null);
    }

    @Then("the search results contain {string}")
    public void theSearchResultsContainTheOffer(String title) throws JsonProcessingException {
        assertThat(searchResultTitles()).contains(title);
    }

    @Then("the search results do not contain {string}")
    public void theSearchResultsDoNotContainTheOffer(String title) throws JsonProcessingException {
        assertThat(searchResultTitles()).doesNotContain(title);
    }

    @Then("the search results contain {string} at about {int} meters")
    public void theSearchResultsContain(String title, int meters) throws JsonProcessingException {
        var result = StreamSupport.stream(http.json(http.last()).path("content").spliterator(), false)
                .filter(offer -> title.equals(offer.path(TITLE).asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No result titled " + title + ": " + http.last().body()));
        assertThat(result.path("distanceMeters").asInt()).isBetween(meters - DISTANCE_TOLERANCE_METERS,
                meters + DISTANCE_TOLERANCE_METERS);
    }

    @And("business {long} has no campaigns")
    public void businessHasNoCampaigns(Long businessId) {
        assertThat(campaigns.findByBusinessId(businessId)).isEmpty();
    }

    private String tokenFor(String credentials) {
        if (WITHOUT_TOKEN.equals(credentials)) {
            return null;
        }
        if (CONSUMER_TOKEN.equals(credentials)) {
            return TestIdentity.consumerToken(CONSUMER_ID);
        }
        return TestIdentity.businessOwnerTokenWithoutBusinessId(BUSINESS_OWNER_USER_ID);
    }

    private void publish(String offerTitle, Map<String, Object> zone) throws JsonProcessingException {
        var published = http.post(CAMPAIGNS, token, campaignBody(DEFAULT_NAME, DEFAULT_START, DEFAULT_END, zone,
                offerTitle, DEFAULT_VALID_TO));
        assertThat(published.status()).as(published.body()).isEqualTo(CREATED);
    }

    private List<String> searchResultTitles() throws JsonProcessingException {
        assertThat(http.last().status()).as(http.last().body()).isEqualTo(OK);
        return StreamSupport.stream(http.json(http.last()).path("content").spliterator(), false)
                .map(offer -> offer.path(TITLE).asText())
                .toList();
    }

    private GeoPoint store() {
        return new GeoPoint(storeLatitude, storeLongitude);
    }

    private GeoPoint storeOffset(int meters, String direction) {
        var latitudeDegrees = meters / METERS_PER_DEGREE;
        var longitudeDegrees = latitudeDegrees / Math.cos(Math.toRadians(storeLatitude));
        return switch (direction) {
            case NORTH -> new GeoPoint(storeLatitude + latitudeDegrees, storeLongitude);
            case SOUTH -> new GeoPoint(storeLatitude - latitudeDegrees, storeLongitude);
            case EAST -> new GeoPoint(storeLatitude, storeLongitude + longitudeDegrees);
            default -> throw new IllegalArgumentException("Unknown direction " + direction);
        };
    }

    private Map<String, Object> radiusZone(int radiusMeters) {
        return Map.of("type", "RADIUS", "center", point(store()), "radiusMeters", radiusMeters);
    }

    private static Map<String, Object> point(GeoPoint point) {
        return Map.of("latitude", point.latitude(), "longitude", point.longitude());
    }

    private String campaignBody(String name, String start, String end, Map<String, Object> zone, String offerTitle,
                                String validTo) throws JsonProcessingException {
        var offer = Map.of("title", offerTitle, "conditions", "Válido de lunes a viernes de 12:00 a 15:00.",
                "price", 15.00, "validTo", validTo, "category", "Gastronomía");
        return objectMapper.writeValueAsString(Map.of(
                "businessName", businessName,
                "name", name,
                "description", "Menú ejecutivo a mitad de precio para oficinas cercanas",
                "period", Map.of("start", start, "end", end),
                "estimatedBudget", Map.of("amount", 500.00, "currency", "PEN"),
                "storeLocation", Map.of("address", storeAddress, "latitude", storeLatitude,
                        "longitude", storeLongitude),
                "zone", zone,
                "offers", List.of(offer)));
    }

    private static long offerIdIn(JsonNode publishedCampaign, String title) {
        return StreamSupport.stream(publishedCampaign.path("offers").spliterator(), false)
                .filter(offer -> title.equals(offer.path(TITLE).asText()))
                .findFirst()
                .orElseThrow()
                .path(OFFER_ID)
                .asLong();
    }
}
