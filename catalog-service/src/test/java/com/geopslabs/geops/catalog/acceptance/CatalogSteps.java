package com.geopslabs.geops.catalog.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.CampaignStatus;
import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.DateRange;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import com.geopslabs.geops.catalog.domain.models.ZoneType;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import com.geopslabs.geops.catalog.infrastructure.persistence.CampaignJpaRepository;
import com.geopslabs.geops.catalog.infrastructure.persistence.MerchantStandingJpaRepository;
import com.geopslabs.geops.catalog.infrastructure.persistence.OfferJpaRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

public class CatalogSteps {
    private static final String OFFERS = "/api/v1/offers";
    private static final String CAMPAIGNS = "/api/v1/campaigns";
    private static final String BY_ID = "/%s";
    private static final String CAMPAIGN_OFFERS = CAMPAIGNS + "/%d/offers";
    private static final Long BUSINESS_OWNER_USER_ID = 9001L;
    private static final Long UNKNOWN_ID = 999_999L;
    private static final Integer RADIUS_METERS = 800;
    private static final BigDecimal BUDGET = new BigDecimal("500.00");
    private static final BigDecimal FULL_COMPLIANCE = new BigDecimal("100.00");
    private static final int NO_REPORTS = 0;
    private static final String CONDITIONS = "De lunes a viernes de 12:00 a 15:00";
    private static final String CATEGORY = "Gastronomía";
    private static final String ADDRESS = "Jr. Huánuco 1250, La Victoria";
    private static final String CAMPAIGN_ID_FIELD = "campaignId";
    private static final String OFFER_ID_FIELD = "offerId";

    private final HttpScenario http;
    private final ScenarioState state;
    private final MerchantStandingRepositoryPort standings;
    private final CampaignRepositoryPort campaigns;
    private final OfferRepositoryPort offers;
    private final OfferJpaRepository offerRows;
    private final CampaignJpaRepository campaignRows;
    private final MerchantStandingJpaRepository standingRows;

    @SuppressWarnings("java:S107")
    public CatalogSteps(HttpScenario http, ScenarioState state, MerchantStandingRepositoryPort standings,
                        CampaignRepositoryPort campaigns, OfferRepositoryPort offers, OfferJpaRepository offerRows,
                        CampaignJpaRepository campaignRows, MerchantStandingJpaRepository standingRows) {
        this.http = http;
        this.state = state;
        this.standings = standings;
        this.campaigns = campaigns;
        this.offers = offers;
        this.offerRows = offerRows;
        this.campaignRows = campaignRows;
        this.standingRows = standingRows;
    }

    @Before
    public void startWithAnEmptyCatalog() {
        offerRows.deleteAll();
        campaignRows.deleteAll();
        standingRows.deleteAll();
    }

    @Given("business {long} named {string} has the campaign {string} from {string} to {string}")
    public void businessHasCampaign(Long businessId, String businessName, String name, String start, String end) {
        standings.save(new MerchantStanding(businessId, businessName, false, NO_REPORTS, FULL_COMPLIANCE, false,
                LocalDateTime.now()));
        var period = new DateRange(LocalDate.parse(start), LocalDate.parse(end));
        var zone = new CampaignZone(ZoneType.RADIUS, RADIUS_METERS, null);
        var campaign = campaigns.save(new Campaign(null, businessId, name, name, period, zone,
                CampaignStatus.ACTIVE, Money.soles(BUDGET)));
        state.rememberCampaign(name, campaign.getId());
    }

    @And("the campaign {string} publishes the offer {string} at {bigdecimal} until {string}")
    public void campaignPublishesOffer(String campaignName, String title, BigDecimal price, String validTo) {
        var campaign = campaigns.findById(state.campaignId(campaignName)).orElseThrow();
        var offer = offers.save(new Offer(null, campaign.getId(), campaign.getBusinessId(), title, CONDITIONS,
                Money.soles(price), LocalDate.parse(validTo), CATEGORY, GeocodingStatus.GEOCODED, ADDRESS, null,
                OfferSource.AFFILIATED, null, OfferStatus.PUBLISHED));
        state.rememberOffer(title, offer.getId());
    }

    @When("a visitor views the offer {string}")
    public void visitorViewsOffer(String title) {
        http.get(OFFERS + BY_ID.formatted(state.offerId(title)), null);
    }

    @When("a visitor views an offer that does not exist")
    public void visitorViewsUnknownOffer() {
        http.get(OFFERS + BY_ID.formatted(UNKNOWN_ID), null);
    }

    @When("a visitor views the offer with id {string}")
    public void visitorViewsOfferWithId(String offerId) {
        http.get(OFFERS + BY_ID.formatted(offerId), null);
    }

    @When("business {long} lists its campaigns")
    public void businessListsCampaigns(Long businessId) {
        http.get(CAMPAIGNS, businessToken(businessId));
    }

    @When("business {long} views the campaign {string}")
    public void businessViewsCampaign(Long businessId, String name) {
        http.get(CAMPAIGNS + BY_ID.formatted(state.campaignId(name)), businessToken(businessId));
    }

    @When("business {long} views a campaign that does not exist")
    public void businessViewsUnknownCampaign(Long businessId) {
        http.get(CAMPAIGNS + BY_ID.formatted(UNKNOWN_ID), businessToken(businessId));
    }

    @When("business {long} lists the offers of the campaign {string}")
    public void businessListsCampaignOffers(Long businessId, String name) {
        http.get(CAMPAIGN_OFFERS.formatted(state.campaignId(name)), businessToken(businessId));
    }

    @When("consumer {long} lists the campaigns")
    public void consumerListsCampaigns(Long consumerId) {
        http.get(CAMPAIGNS, TestIdentity.consumerToken(consumerId));
    }

    @When("a visitor lists the campaigns")
    public void visitorListsCampaigns() {
        http.get(CAMPAIGNS, null);
    }

    @And("the list only has the campaign {string}")
    public void listOnlyHasCampaign(String name) throws JsonProcessingException {
        assertThat(idsOf(http.json(http.last()), CAMPAIGN_ID_FIELD)).containsExactly(state.campaignId(name));
    }

    @And("the list only has the offer {string}")
    public void listOnlyHasOffer(String title) throws JsonProcessingException {
        assertThat(idsOf(http.json(http.last()), OFFER_ID_FIELD)).containsExactly(state.offerId(title));
    }

    private static String businessToken(Long businessId) {
        return TestIdentity.businessOwnerToken(BUSINESS_OWNER_USER_ID, businessId);
    }

    private static Long[] idsOf(JsonNode list, String field) {
        return StreamSupport.stream(list.spliterator(), false)
                .map(item -> item.path(field).asLong())
                .toArray(Long[]::new);
    }
}
