package com.geopslabs.geops.catalog.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps of the Sprint 2 nearby search (US08, US14, US39, US46), written as the consumer says them.
 */
public class NearbySearchSteps {
    static final String NEARBY = "/api/v1/offers/nearby?lat=%s&lng=%s&radiusMinutes=%d";
    private static final String CATEGORY = "&category=%s";
    private static final double METERS_PER_DEGREE = Math.toRadians(1) * GeoPoint.EARTH_MEAN_RADIUS_METERS;
    private static final BigDecimal PRICE = new BigDecimal("15.00");
    private static final BigDecimal FULL_COMPLIANCE = new BigDecimal("100.00");
    private static final String CONDITIONS = "Un cupón por mesa";
    private static final String ADDRESS = "Av. Larco 345, Miraflores";
    private static final String DEFAULT_CATEGORY = "Gastronomía";
    private static final int CAMPAIGN_RADIUS_METERS = 1_600;
    private static final int DAYS_LEFT = 10;
    private static final long FIRST_BUSINESS_ID = 300L;
    private static final int METERS_BETWEEN_OFFERS = 20;
    private static final int OK = 200;
    private static final String TITLE = "title";
    private static final String CONTENT = "content";
    private static final String TITLE_SEPARATOR = ",";

    private final HttpScenario http;
    private final ScenarioState state;
    private final OfferRepositoryPort offers;
    private final CampaignRepositoryPort campaigns;
    private final MerchantStandingRepositoryPort standings;
    private final Clock clock;

    public NearbySearchSteps(HttpScenario http, ScenarioState state, OfferRepositoryPort offers,
                             CampaignRepositoryPort campaigns, MerchantStandingRepositoryPort standings, Clock clock) {
        this.http = http;
        this.state = state;
        this.offers = offers;
        this.campaigns = campaigns;
        this.standings = standings;
        this.clock = clock;
    }

    @Given("{int} valid offers exist around the origin and only number {int} is of category {string}")
    public void validOffersWithOneOfCategory(int count, int number, String category) {
        var today = LocalDate.now(clock);
        for (int index = 1; index <= count; index++) {
            var businessId = FIRST_BUSINESS_ID + index;
            var title = "Oferta " + index;
            standings.save(new MerchantStanding(businessId, title, false, 0, FULL_COMPLIANCE, false,
                    clock.instant()));
            var campaignId = campaigns.save(new Campaign(null, businessId, title, title,
                    new DateRange(today.minusDays(1), today.plusDays(DAYS_LEFT)),
                    new CampaignZone(ZoneType.RADIUS, null, CAMPAIGN_RADIUS_METERS, null), CampaignStatus.ACTIVE,
                    Money.soles(PRICE))).getId();
            offers.save(new Offer(null, campaignId, businessId, title, CONDITIONS, Money.soles(PRICE),
                    today.plusDays(DAYS_LEFT), index == number ? category : DEFAULT_CATEGORY,
                    GeocodingStatus.GEOCODED, ADDRESS, null, OfferSource.AFFILIATED, null, OfferStatus.PUBLISHED,
                    north(index * METERS_BETWEEN_OFFERS)));
        }
    }

    @When("I search within {int} minutes with category {string}")
    public void searchWithCategory(int minutes, String category) {
        // RestClient encodes the template, accents included.
        http.get(nearby(minutes) + CATEGORY.formatted(category), null);
    }

    @When("I search within {int} minutes")
    public void search(int minutes) {
        http.get(nearby(minutes), null);
        assertThat(http.last().status()).as(http.last().body()).isEqualTo(OK);
    }

    @When("I search within {int} minutes again")
    public void searchAgain(int minutes) {
        search(minutes);
    }

    @Given("I searched within {int} minutes once")
    public void searchedOnce(int minutes) {
        search(minutes);
    }

    @Then("the titles are {string} in that order")
    public void titlesAre(String titles) throws JsonProcessingException {
        var expected = Arrays.stream(titles.split(TITLE_SEPARATOR)).map(String::strip).toList();
        assertThat(titles()).containsExactlyElementsOf(expected);
    }

    @Then("the result is empty")
    public void resultIsEmpty() throws JsonProcessingException {
        assertThat(titles()).isEmpty();
        assertThat(http.json(http.last()).path("totalElements").asInt()).isZero();
    }

    @Then("the result has {string}")
    public void resultHas(String title) throws JsonProcessingException {
        assertThat(titles()).contains(title);
    }

    @Then("the result does not have {string}")
    public void resultDoesNotHave(String title) throws JsonProcessingException {
        assertThat(titles()).doesNotContain(title);
    }

    @Then("the first page has {string}")
    public void firstPageHas(String title) throws JsonProcessingException {
        assertThat(http.json(http.last()).path("page").asInt()).isZero();
        assertThat(titles()).containsExactly(title);
    }

    JsonNode resultTitled(String title) throws JsonProcessingException {
        return StreamSupport.stream(http.json(http.last()).path(CONTENT).spliterator(), false)
                .filter(node -> title.equals(node.path(TITLE).asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No result titled " + title + ": " + http.last().body()));
    }

    private List<String> titles() throws JsonProcessingException {
        return StreamSupport.stream(http.json(http.last()).path(CONTENT).spliterator(), false)
                .map(node -> node.path(TITLE).asText())
                .toList();
    }

    private String nearby(int minutes) {
        var origin = state.origin();
        return NEARBY.formatted(origin.latitude(), origin.longitude(), minutes);
    }

    private GeoPoint north(double meters) {
        var origin = state.origin();
        return new GeoPoint(origin.latitude() + meters / METERS_PER_DEGREE, origin.longitude());
    }
}
