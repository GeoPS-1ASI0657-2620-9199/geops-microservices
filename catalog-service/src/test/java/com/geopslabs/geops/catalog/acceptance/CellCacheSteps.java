package com.geopslabs.geops.catalog.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.micrometer.core.instrument.MeterRegistry;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps of the cell cache (US14) and of the offers each scenario places around the origin.
 */
public class CellCacheSteps {
    private static final double METERS_PER_DEGREE = Math.toRadians(1) * GeoPoint.EARTH_MEAN_RADIUS_METERS;
    private static final long DEFAULT_BUSINESS_ID = 84L;
    private static final String DEFAULT_BUSINESS_NAME = "Restaurante Don Pepe";
    private static final BigDecimal PRICE = new BigDecimal("15.00");
    private static final BigDecimal FULL_COMPLIANCE = new BigDecimal("100.00");
    private static final String CONDITIONS = "Un cupón por mesa";
    private static final String CATEGORY = "Gastronomía";
    private static final String ADDRESS = "Av. Larco 345, Miraflores";
    private static final int DAYS_LEFT = 2;
    private static final int CAMPAIGN_DAYS = 10;
    private static final int CAMPAIGN_RADIUS_METERS = 1_000;
    private static final long OWNER_USER_ID = 9001L;
    private static final int CREATED = 201;
    private static final String HITS = "catalog.cell.cache.hits";
    private static final String MISSES = "catalog.cell.cache.misses";

    private final ScenarioState state;
    private final HttpScenario http;
    private final OfferRepositoryPort offers;
    private final CampaignRepositoryPort campaigns;
    private final MerchantStandingRepositoryPort standings;
    private final MutableClock clock;
    private final MeterRegistry meters;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("java:S107")
    public CellCacheSteps(ScenarioState state, HttpScenario http, OfferRepositoryPort offers,
                          CampaignRepositoryPort campaigns, MerchantStandingRepositoryPort standings,
                          MutableClock clock, MeterRegistry meters, ObjectMapper objectMapper) {
        this.state = state;
        this.http = http;
        this.offers = offers;
        this.campaigns = campaigns;
        this.standings = standings;
        this.clock = clock;
        this.meters = meters;
        this.objectMapper = objectMapper;
    }

    @Given("a valid offer {string} exists {int} meters north of the origin")
    public void validOfferExists(String title, int meters) {
        businessHasOffer(DEFAULT_BUSINESS_ID, DEFAULT_BUSINESS_NAME, title, meters);
    }

    @Given("business {long} {string} has offer {string} {int} meters north of the origin")
    public void businessHasOffer(Long businessId, String businessName, String title, int meters) {
        var today = LocalDate.now(clock);
        if (standings.findByBusinessId(businessId).isEmpty()) {
            standings.save(new MerchantStanding(businessId, businessName, false, 0, FULL_COMPLIANCE, false,
                    clock.instant().minus(Duration.ofDays(1))));
        }
        var campaignId = campaigns.save(new Campaign(null, businessId, title, title,
                new DateRange(today.minusDays(1), today.plusDays(CAMPAIGN_DAYS)),
                new CampaignZone(ZoneType.RADIUS, null, CAMPAIGN_RADIUS_METERS, null), CampaignStatus.ACTIVE,
                Money.soles(PRICE))).getId();
        var offer = offers.save(new Offer(null, campaignId, businessId, title, CONDITIONS, Money.soles(PRICE),
                today.plusDays(DAYS_LEFT), CATEGORY, GeocodingStatus.GEOCODED, ADDRESS, null, OfferSource.AFFILIATED,
                null, OfferStatus.PUBLISHED, north(meters)));
        state.rememberOffer(title, offer.getId());
    }

    @When("business {long} publishes a campaign with offer {string} {int} meters north of the origin")
    public void businessPublishes(Long businessId, String title, int meters) throws JsonProcessingException {
        var today = LocalDate.now(clock);
        var store = north(meters);
        var point = Map.of("latitude", store.latitude(), "longitude", store.longitude());
        var body = Map.of(
                "businessName", DEFAULT_BUSINESS_NAME,
                "name", title,
                "description", title,
                "period", Map.of("start", today.toString(), "end", today.plusDays(CAMPAIGN_DAYS).toString()),
                "storeLocation", Map.of("address", ADDRESS, "latitude", store.latitude(),
                        "longitude", store.longitude()),
                "zone", Map.of("type", "RADIUS", "center", point, "radiusMeters", CAMPAIGN_RADIUS_METERS),
                "offers", List.of(Map.of("title", title, "conditions", CONDITIONS, "price", PRICE,
                        "validTo", today.plusDays(DAYS_LEFT).toString(), "category", CATEGORY)));
        var token = TestIdentity.businessOwnerToken(OWNER_USER_ID, businessId);
        var created = http.post("/api/v1/campaigns", token, objectMapper.writeValueAsString(body));
        assertThat(created.status()).as(created.body()).isEqualTo(CREATED);
    }

    @Given("the date moves to the day after {string} expires")
    public void dateMovesPastExpiry(String title) {
        var validTo = offers.findById(state.offerId(title)).orElseThrow().getValidTo();
        while (!LocalDate.now(clock).isAfter(validTo)) {
            clock.advance(Duration.ofDays(1));
        }
    }

    @Given("the cache is unavailable")
    public void cacheIsUnavailable() {
        RedisControl.pause();
    }

    @Then("the database was not queried for the cells of the second search")
    public void databaseWasNotQueried() {
        assertThat(count(MISSES)).as("cells read from PostgreSQL").isEqualTo(state.missesAfterFirstSearch());
        assertThat(count(HITS)).as("cells served from Redis").isGreaterThan(state.hitsAfterFirstSearch());
    }

    double count(String counter) {
        return meters.counter(counter).count();
    }

    private GeoPoint north(double meters) {
        var origin = state.origin();
        return new GeoPoint(origin.latitude() + meters / METERS_PER_DEGREE, origin.longitude());
    }
}
