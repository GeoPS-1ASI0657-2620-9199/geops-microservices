package com.geopslabs.geops.catalog.acceptance;

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
import com.geopslabs.geops.catalog.domain.models.ZoneType;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CatalogFixtures {
    private static final LocalDate CAMPAIGN_START = LocalDate.parse("2026-10-01");
    private static final LocalDate CAMPAIGN_END = LocalDate.parse("2026-10-31");
    private static final String CAMPAIGN_NAME = "Almuerzos de octubre";
    private static final Integer RADIUS_METERS = 800;
    private static final BigDecimal BUDGET = new BigDecimal("500.00");
    private static final BigDecimal FULL_COMPLIANCE = new BigDecimal("100.00");
    private static final BigDecimal PRICE = new BigDecimal("15.00");
    private static final int NO_REPORTS = 0;
    private static final String CONDITIONS = "Válido de lunes a viernes de 12:00 a 15:00. Un cupón por mesa.";
    private static final String CATEGORY = "Gastronomía";
    private static final String ADDRESS = "Av. Larco 345, Miraflores";
    private static final GeoPoint STORE = new GeoPoint(-12.1211, -77.0297);
    private static final String OFFER_TITLE = "Oferta de prueba";

    private final ScenarioState state;
    private final MerchantStandingRepositoryPort standings;
    private final CampaignRepositoryPort campaigns;
    private final OfferRepositoryPort offers;

    public CatalogFixtures(ScenarioState state, MerchantStandingRepositoryPort standings,
                           CampaignRepositoryPort campaigns, OfferRepositoryPort offers) {
        this.state = state;
        this.standings = standings;
        this.campaigns = campaigns;
        this.offers = offers;
    }

    @Given("business {long} {string} has an active campaign")
    public void businessHasAnActiveCampaign(Long businessId, String businessName) {
        standings.save(new MerchantStanding(businessId, businessName, false, NO_REPORTS, FULL_COMPLIANCE, false,
                LocalDateTime.now()));
        var campaign = campaigns.save(new Campaign(null, businessId, CAMPAIGN_NAME, CAMPAIGN_NAME,
                new DateRange(CAMPAIGN_START, CAMPAIGN_END), new CampaignZone(ZoneType.RADIUS, RADIUS_METERS, null),
                CampaignStatus.ACTIVE, Money.soles(BUDGET)));
        state.rememberCampaignOfBusiness(businessId, campaign.getId());
    }

    @Given("the campaign of business {long} has the offer {string} valid until {string}")
    public void campaignHasTheOffer(Long businessId, String title, String validTo) {
        publish(businessId, title, PRICE, LocalDate.parse(validTo), CONDITIONS, ADDRESS, STORE, OfferStatus.PUBLISHED);
    }

    @Given("the campaign of business {long} has this offer:")
    public void campaignHasThisOffer(Long businessId, DataTable table) {
        var fields = table.asMap();
        var location = new GeoPoint(Double.parseDouble(fields.get("latitude")),
                Double.parseDouble(fields.get("longitude")));
        publish(businessId, fields.get("title"), new BigDecimal(fields.get("price")),
                LocalDate.parse(fields.get("validTo")), fields.get("conditions"), fields.get("address"), location,
                OfferStatus.PUBLISHED);
    }

    @Given("the campaign of business {long} has an offer valid until {string} with status {word}")
    public void campaignHasAnOfferWithStatus(Long businessId, String validTo, String status) {
        publish(businessId, OFFER_TITLE, PRICE, LocalDate.parse(validTo), CONDITIONS, ADDRESS, STORE,
                OfferStatus.valueOf(status));
    }

    @SuppressWarnings("java:S107")
    private void publish(Long businessId, String title, BigDecimal price, LocalDate validTo, String conditions,
                         String address, GeoPoint location, OfferStatus status) {
        var offer = offers.save(new Offer(null, state.campaignIdOfBusiness(businessId), businessId, title,
                conditions, Money.soles(price), validTo, CATEGORY, GeocodingStatus.GEOCODED, address, null,
                OfferSource.AFFILIATED, null, status, location));
        state.rememberCurrentOffer(offer);
    }
}
