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
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidGeoPointException;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class ProximitySearchSteps {
    private static final String SEED_SCRIPT = "seed/proximity-10000-offers.sql";
    private static final long SEED_ID = 9001L;
    private static final String DELETE_SEED_OFFERS = "DELETE FROM offers WHERE campaign_id = ?";
    private static final String DELETE_SEED_CAMPAIGN = "DELETE FROM campaigns WHERE id = ?";
    private static final String DELETE_SEED_STANDING = "DELETE FROM merchant_standings WHERE business_id = ?";
    private static final String EXPLAIN_RADIUS_SEARCH = """
            EXPLAIN (FORMAT JSON)
            SELECT o.id FROM offers o
            WHERE ST_DWithin(o.location, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?, false)""";
    private static final String NEARBY = "/api/v1/offers/nearby?lat=%s&lng=%s";
    private static final String RADIUS = "&radiusMinutes=%d";
    private static final String SIZE = "&size=%d";
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final double METERS_PER_DEGREE = Math.toRadians(1) * GeoPoint.EARTH_MEAN_RADIUS_METERS;
    private static final double SAME_COORDINATE = 1e-9;
    private static final double PERCENTILE_95 = 0.95;
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final int REPEATED_SEARCHES = 100;
    private static final int CAMPAIGN_RADIUS_METERS = 800;
    private static final int DAYS_LEFT = 10;
    private static final long BUSINESS_ID = 84L;
    private static final String BUSINESS_NAME = "Cevichería Doña Rosa";
    private static final String VALID = "valid";
    private static final String ACTIVE = "active";
    private static final String CONDITIONS = "Un cupón por mesa";
    private static final BigDecimal PRICE = new BigDecimal("15.00");
    private static final BigDecimal FULL_COMPLIANCE = new BigDecimal("100.00");
    private static final LocalDate VALID_TO = LocalDate.of(2026, 10, 31);
    private static final String CATEGORY = "Gastronomía";
    private static final String ADDRESS = "Av. Larco 345, Miraflores";
    private static final String SOURCE_NAME = "Directorio público";
    private static final String CONTENT = "content";
    private static final String TITLE = "title";
    private static final String METERS_NORTH = "meters north";
    private static final String OPEN_REPORTS = "open reports";
    private static final long FIRST_NEARBY_BUSINESS_ID = 100L;
    private static final String NO_REPORTS = "0";

    private final OfferRepositoryPort offers;
    private final CampaignRepositoryPort campaigns;
    private final MerchantStandingRepositoryPort standings;
    private final HttpScenario http;
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private GeoPoint origin;
    private Long savedOfferId;
    private Offer readOffer;
    private String plan;
    private InvalidGeoPointException rejection;
    private boolean seeded;
    private final List<Long> responseMillis = new ArrayList<>();

    @SuppressWarnings("java:S107")
    public ProximitySearchSteps(OfferRepositoryPort offers, CampaignRepositoryPort campaigns,
                                MerchantStandingRepositoryPort standings, HttpScenario http, JdbcTemplate jdbc,
                                DataSource dataSource) {
        this.offers = offers;
        this.campaigns = campaigns;
        this.standings = standings;
        this.http = http;
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    @After
    public void removeTheMeasurementOffers() {
        if (seeded) {
            jdbc.update(DELETE_SEED_OFFERS, SEED_ID);
            jdbc.update(DELETE_SEED_CAMPAIGN, SEED_ID);
            jdbc.update(DELETE_SEED_STANDING, SEED_ID);
        }
    }

    @Given("the offer {string} is saved at latitude {double} and longitude {double}")
    public void offerIsSavedAt(String title, double latitude, double longitude) {
        var offer = new Offer(null, null, null, title, CONDITIONS, Money.soles(PRICE), VALID_TO, CATEGORY,
                GeocodingStatus.GEOCODED, ADDRESS, null, OfferSource.PUBLIC_SOURCE, SOURCE_NAME,
                OfferStatus.PUBLISHED, new GeoPoint(latitude, longitude));
        savedOfferId = offers.save(offer).getId();
    }

    @When("the offer is read from the catalog")
    public void offerIsRead() {
        readOffer = offers.findById(savedOfferId).orElseThrow();
    }

    @Then("its location has latitude {double} and longitude {double}")
    public void locationIs(double latitude, double longitude) {
        assertThat(readOffer.getLocation().latitude()).isCloseTo(latitude, within(SAME_COORDINATE));
        assertThat(readOffer.getLocation().longitude()).isCloseTo(longitude, within(SAME_COORDINATE));
    }

    @Given("the catalog has the 10000 measurement offers spread over Lima")
    public void catalogHasMeasurementOffers() {
        new ResourceDatabasePopulator(new ClassPathResource(SEED_SCRIPT)).execute(dataSource);
        seeded = true;
    }

    @When("the execution plan of a search within {int} meters of {double}, {double} is read")
    public void executionPlanIsRead(int meters, double latitude, double longitude) {
        plan = jdbc.queryForObject(EXPLAIN_RADIUS_SEARCH, String.class, longitude, latitude, meters);
    }

    @Then("the plan uses the index {string}")
    public void planUsesIndex(String index) {
        assertThat(plan).contains(index);
    }

    @When("an offer is registered at latitude {double} and longitude {double}")
    public void offerIsRegisteredAt(double latitude, double longitude) {
        try {
            new GeoPoint(latitude, longitude);
        } catch (InvalidGeoPointException exception) {
            rejection = exception;
        }
    }

    @Then("the registration is rejected with the reason {string}")
    public void registrationIsRejected(String reason) {
        assertThat(rejection).isNotNull().hasMessage(reason);
    }

    @Given("the search origin is latitude {double} and longitude {double}")
    public void searchOriginIs(double latitude, double longitude) {
        origin = new GeoPoint(latitude, longitude);
    }

    @Given("these offers exist around the origin:")
    public void theseOffersExist(DataTable table) {
        var today = LocalDate.now(LIMA);
        standings.save(new MerchantStanding(BUSINESS_ID, BUSINESS_NAME, true, 0, FULL_COMPLIANCE, true,
                LocalDateTime.now()));
        var period = new DateRange(today.minusDays(1), today.plusDays(DAYS_LEFT));
        var activeCampaign = campaign(period, CampaignStatus.ACTIVE);
        var pausedCampaign = campaign(period, CampaignStatus.PAUSED);
        for (Map<String, String> row : table.asMaps()) {
            var validTo = VALID.equals(row.get("validity")) ? today.plusDays(DAYS_LEFT) : today.minusDays(1);
            var campaignId = ACTIVE.equals(row.get("campaign")) ? activeCampaign : pausedCampaign;
            var location = new GeoPoint(origin.latitude() + Double.parseDouble(row.get("meters north")) / METERS_PER_DEGREE,
                    origin.longitude());
            offers.save(new Offer(null, campaignId, BUSINESS_ID, row.get(TITLE), CONDITIONS, Money.soles(PRICE),
                    validTo, CATEGORY, GeocodingStatus.GEOCODED, ADDRESS, null, OfferSource.AFFILIATED, null,
                    OfferStatus.PUBLISHED, location));
        }
    }

    @When("I search nearby offers with radiusMinutes {int}")
    public void searchWithRadius(int minutes) {
        http.get(nearby(origin) + RADIUS.formatted(minutes), null);
    }

    @When("I search nearby offers from latitude {double} and longitude {double} with radiusMinutes {int}")
    public void searchFrom(double latitude, double longitude, int minutes) {
        http.get(NEARBY.formatted(latitude, longitude) + RADIUS.formatted(minutes), null);
    }

    @When("I search nearby offers without radiusMinutes")
    public void searchWithoutRadius() {
        http.get(nearby(origin), null);
    }

    @When("I search nearby offers with radiusMinutes {int} and page size {int}")
    public void searchWithPageSize(int minutes, int size) {
        http.get(nearby(origin) + RADIUS.formatted(minutes) + SIZE.formatted(size), null);
    }

    @When("I search nearby offers with radiusMinutes {int} one hundred times in a row")
    public void searchOneHundredTimes(int minutes) {
        var path = nearby(origin) + RADIUS.formatted(minutes);
        for (int request = 0; request < REPEATED_SEARCHES; request++) {
            var start = System.nanoTime();
            http.get(path, null);
            responseMillis.add((System.nanoTime() - start) / NANOS_PER_MILLI);
        }
    }

    @Then("the response contains only the offer {string}")
    public void responseContainsOnly(String title) throws JsonProcessingException {
        assertThat(titles()).containsExactly(title);
    }

    @And("the offer {string} is {int} meters and {int} minutes on foot away")
    public void offerIsAt(String title, int meters, int minutes) throws JsonProcessingException {
        var offer = contentStream().filter(node -> title.equals(node.path(TITLE).asText())).findFirst().orElseThrow();
        assertThat(offer.path("distanceMeters").asInt()).isEqualTo(meters);
        assertThat(offer.path("walkMinutes").asInt()).isEqualTo(minutes);
    }

    @Then("the 95th percentile of the response time is under {int} milliseconds")
    public void percentileIsUnder(int millis) {
        var sorted = responseMillis.stream().sorted().toList();
        var p95 = sorted.get((int) Math.ceil(PERCENTILE_95 * sorted.size()) - 1);
        assertThat(p95).as("p95 of %d requests", sorted.size()).isLessThan(millis);
    }

    @Given("these valid offers exist around the origin, each from its own business:")
    public void theseValidOffersExist(DataTable table) {
        var today = LocalDate.now(LIMA);
        var period = new DateRange(today.minusDays(1), today.plusDays(DAYS_LEFT));
        var businessId = FIRST_NEARBY_BUSINESS_ID;
        for (Map<String, String> row : table.asMaps()) {
            var openReports = Integer.parseInt(row.getOrDefault(OPEN_REPORTS, NO_REPORTS));
            standings.save(new MerchantStanding(businessId, row.get(TITLE), false, openReports, FULL_COMPLIANCE, false,
                    LocalDateTime.now()));
            var campaignId = campaigns.save(new Campaign(null, businessId, row.get(TITLE), row.get(TITLE), period,
                    new CampaignZone(ZoneType.RADIUS, null, CAMPAIGN_RADIUS_METERS, null), CampaignStatus.ACTIVE,
                    Money.soles(PRICE))).getId();
            offers.save(new Offer(null, campaignId, businessId, row.get(TITLE), CONDITIONS, Money.soles(PRICE),
                    today.plusDays(DAYS_LEFT), CATEGORY, GeocodingStatus.GEOCODED, ADDRESS, null,
                    OfferSource.AFFILIATED, null, OfferStatus.PUBLISHED, north(row.get(METERS_NORTH))));
            businessId++;
        }
    }

    @Given("the consumer denies the location permission and picks a district centered at the origin")
    public void consumerPicksDistrict() {
        assertThat(origin).isNotNull();
    }

    @When("I search nearby offers from the district center with radiusMinutes {int}")
    public void searchFromDistrictCenter(int minutes) {
        searchWithRadius(minutes);
    }

    @Given("there are no valid offers within {int} meters of the origin")
    public void noValidOffersNearby(int meters) {
        assertThat(offers.findPublishedWithin(origin, meters, LocalDate.now(LIMA))).isEmpty();
    }

    @Then("the offers come in this order:")
    public void offersComeInOrder(DataTable expected) throws JsonProcessingException {
        var actual = contentStream()
                .map(node -> List.of(node.path(TITLE).asText(), node.path("distanceMeters").asText(),
                        node.path("walkMinutes").asText()))
                .toList();
        var rows = expected.asMaps().stream()
                .map(row -> List.of(row.get(TITLE), row.get("distanceMeters"), row.get("walkMinutes")))
                .toList();
        assertThat(actual).isEqualTo(rows);
    }

    @Then("the list is empty with totalElements {int}")
    public void listIsEmpty(int total) throws JsonProcessingException {
        assertThat(titles()).isEmpty();
        assertThat(http.json(http.last()).path("totalElements").asInt()).isEqualTo(total);
    }

    private GeoPoint north(String meters) {
        return new GeoPoint(origin.latitude() + Double.parseDouble(meters) / METERS_PER_DEGREE, origin.longitude());
    }

    private Long campaign(DateRange period, CampaignStatus status) {
        return campaigns.save(new Campaign(null, BUSINESS_ID, status.name(), status.name(), period,
                new CampaignZone(ZoneType.RADIUS, null, CAMPAIGN_RADIUS_METERS, null), status, Money.soles(PRICE))).getId();
    }

    private static String nearby(GeoPoint point) {
        return NEARBY.formatted(point.latitude(), point.longitude());
    }

    private List<String> titles() throws JsonProcessingException {
        return contentStream().map(node -> node.path(TITLE).asText()).toList();
    }

    private Stream<JsonNode> contentStream() throws JsonProcessingException {
        return StreamSupport.stream(http.json(http.last()).path(CONTENT).spliterator(), false);
    }
}
