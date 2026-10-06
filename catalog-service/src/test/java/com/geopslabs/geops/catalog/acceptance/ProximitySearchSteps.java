package com.geopslabs.geops.catalog.acceptance;

import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidGeoPointException;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;

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
    private static final double SAME_COORDINATE = 1e-9;
    private static final String CONDITIONS = "Un cupón por mesa";
    private static final BigDecimal PRICE = new BigDecimal("15.00");
    private static final LocalDate VALID_TO = LocalDate.of(2026, 10, 31);
    private static final String CATEGORY = "Gastronomía";
    private static final String ADDRESS = "Av. Larco 345, Miraflores";
    private static final String SOURCE_NAME = "Directorio público";

    private final OfferRepositoryPort offers;
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private Long savedOfferId;
    private Offer readOffer;
    private String plan;
    private InvalidGeoPointException rejection;
    private boolean seeded;

    public ProximitySearchSteps(OfferRepositoryPort offers, JdbcTemplate jdbc, DataSource dataSource) {
        this.offers = offers;
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
}
