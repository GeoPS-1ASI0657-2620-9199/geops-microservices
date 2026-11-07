package com.geopslabs.geops.catalog.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.catalog.domain.events.CampaignPaused;
import com.geopslabs.geops.catalog.domain.events.OfferPublished;
import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.CampaignStatus;
import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.DateRange;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import com.geopslabs.geops.catalog.infrastructure.persistence.OutboxEventJpaRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps of US10: the business owner pauses and reactivates a campaign through PATCH /api/v1/campaigns/{id}/status.
 */
public class CampaignStatusSteps {
    private static final double METERS_PER_DEGREE = Math.toRadians(1) * GeoPoint.EARTH_MEAN_RADIUS_METERS;
    private static final String STATUS_PATH = "/api/v1/campaigns/%d/status";
    private static final String AVAILABILITY_PATH = "/internal/v1/offers/%d/availability";
    private static final String BUSINESS_NAME = "Restaurante Don Pepe";
    private static final BigDecimal PRICE = new BigDecimal("18.50");
    private static final BigDecimal FULL_COMPLIANCE = new BigDecimal("100.00");
    private static final String CONDITIONS = "Lunes a viernes de 12:00 a 15:00";
    private static final String CATEGORY = "Gastronomía";
    private static final String ADDRESS = "Av. Larco 345, Miraflores";
    private static final int CAMPAIGN_RADIUS_METERS = 1_200;
    private static final int STARTED_DAYS_AGO = 1;
    private static final long OWNER_USER_ID = 9001L;
    private static final int OK = 200;

    private final ScenarioState state;
    private final HttpScenario http;
    private final OfferRepositoryPort offers;
    private final CampaignRepositoryPort campaigns;
    private final MerchantStandingRepositoryPort standings;
    private final OutboxEventJpaRepository outbox;
    private final MutableClock clock;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("java:S107")
    public CampaignStatusSteps(ScenarioState state, HttpScenario http, OfferRepositoryPort offers,
                               CampaignRepositoryPort campaigns, MerchantStandingRepositoryPort standings,
                               OutboxEventJpaRepository outbox, MutableClock clock, ObjectMapper objectMapper) {
        this.state = state;
        this.http = http;
        this.offers = offers;
        this.campaigns = campaigns;
        this.standings = standings;
        this.outbox = outbox;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Given("business {long} has an active campaign {string} until {string} with offer {string} {int} meters north of the origin")
    public void businessHasActiveCampaign(Long businessId, String name, String end, String title, int meters) {
        var today = LocalDate.now(clock);
        var store = north(meters);
        standings.save(new MerchantStanding(businessId, BUSINESS_NAME, false, 0, FULL_COMPLIANCE, false,
                clock.instant()));
        var campaign = campaigns.save(new Campaign(null, businessId, name, name,
                new DateRange(today.minusDays(STARTED_DAYS_AGO), LocalDate.parse(end)),
                CampaignZone.radius(store, CAMPAIGN_RADIUS_METERS), CampaignStatus.ACTIVE, Money.soles(PRICE)));
        var offer = offers.save(new Offer(null, campaign.getId(), businessId, title, CONDITIONS, Money.soles(PRICE),
                LocalDate.parse(end), CATEGORY, GeocodingStatus.GEOCODED, ADDRESS, null, OfferSource.AFFILIATED,
                null, OfferStatus.PUBLISHED, store));
        state.rememberCampaign(name, campaign.getId());
        state.rememberOffer(title, offer.getId());
    }

    @When("business owner of business {long} sets campaign {string} to {string}")
    public void setsCampaignTo(Long businessId, String name, String status) throws JsonProcessingException {
        var token = TestIdentity.businessOwnerToken(OWNER_USER_ID, businessId);
        http.patch(STATUS_PATH.formatted(state.campaignId(name)), token,
                objectMapper.writeValueAsString(Map.of("status", status)));
    }

    @Given("business owner of business {long} set campaign {string} to {string}")
    public void setCampaignTo(Long businessId, String name, String status) throws JsonProcessingException {
        setsCampaignTo(businessId, name, status);
        assertThat(http.last().status()).as(http.last().body()).isEqualTo(OK);
    }

    @And("a CampaignPaused event of campaign {string} with offer {string} is in the outbox")
    public void campaignPausedIsInTheOutbox(String name, String title) throws JsonProcessingException {
        var payloads = payloadsOf(name, CampaignPaused.EVENT_TYPE);
        assertThat(payloads).singleElement().satisfies(payload -> {
            var offerIds = new ArrayList<Long>();
            payload.path("offerIds").forEach(id -> offerIds.add(id.asLong()));
            assertThat(offerIds).containsExactly(state.offerId(title));
        });
    }

    @And("an OfferPublished event of campaign {string} marked as reactivated is in the outbox")
    public void reactivatedOfferPublishedIsInTheOutbox(String name) throws JsonProcessingException {
        var reactivated = payloadsOf(name, OfferPublished.EVENT_TYPE).stream()
                .filter(payload -> payload.path("reactivated").asBoolean())
                .toList();
        assertThat(reactivated).hasSize(1);
    }

    @Then("no CampaignPaused event of campaign {string} is in the outbox")
    public void noCampaignPausedIsInTheOutbox(String name) throws JsonProcessingException {
        assertThat(payloadsOf(name, CampaignPaused.EVENT_TYPE)).isEmpty();
    }

    @When("Reservation asks for the availability of offer {string}")
    public void reservationAsksForOffer(String title) {
        http.get(AVAILABILITY_PATH.formatted(state.offerId(title)), null);
    }

    private List<JsonNode> payloadsOf(String campaign, String eventType) throws JsonProcessingException {
        var payloads = new ArrayList<JsonNode>();
        for (var row : outbox.findByAggregateIdAndEventType(state.campaignId(campaign), eventType)) {
            payloads.add(objectMapper.readTree(row.getPayload()).path("payload"));
        }
        return payloads;
    }

    private GeoPoint north(int meters) {
        var origin = state.origin();
        return new GeoPoint(origin.latitude() + meters / METERS_PER_DEGREE, origin.longitude());
    }
}
