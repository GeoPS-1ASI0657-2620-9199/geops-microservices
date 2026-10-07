package com.geopslabs.geops.catalog.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final String DEFAULT_OFFER = "Oferta de prueba";
    private static final String DEFAULT_START = "2026-10-05";
    private static final String DEFAULT_END = "2026-10-31";
    private static final String DEFAULT_VALID_TO = "2026-10-15";
    private static final double METERS_PER_DEGREE = 111_320;
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
        http.post(CAMPAIGNS, token, campaignBody(name, start, end, radiusMeters, offerTitle, validTo));
    }

    @When("^the campaign \"([^\"]*)\" is sent (without a token|with a consumer token|with a business token without its id)$")
    public void theCampaignIsSent(String name, String credentials) throws JsonProcessingException {
        var body = campaignBody(name, DEFAULT_START, DEFAULT_END, DEFAULT_RADIUS_METERS, DEFAULT_OFFER,
                DEFAULT_VALID_TO);
        http.post(CAMPAIGNS, tokenFor(credentials), body);
    }

    @And("the offer {string} is available in its public detail")
    public void theOfferIsAvailableInItsPublicDetail(String title) throws JsonProcessingException {
        var offerId = offerIdIn(http.json(http.last()), title);
        var detail = http.json(http.get(OFFER_DETAIL.formatted(offerId), null));
        assertThat(detail.path("available").asBoolean()).isTrue();
    }

    @And("a consumer searches for offers {int} meters east of the store within {int} walking minutes")
    public void aConsumerSearchesEastOfTheStore(int meters, int minutes) {
        var longitude = storeLongitude + meters / (METERS_PER_DEGREE * Math.cos(Math.toRadians(storeLatitude)));
        http.get(NEARBY.formatted(storeLatitude, longitude, minutes), null);
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

    private String campaignBody(String name, String start, String end, int radiusMeters, String offerTitle,
                                String validTo) throws JsonProcessingException {
        var store = Map.of("latitude", storeLatitude, "longitude", storeLongitude);
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
                "zone", Map.of("type", "RADIUS", "center", store, "radiusMeters", radiusMeters),
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
