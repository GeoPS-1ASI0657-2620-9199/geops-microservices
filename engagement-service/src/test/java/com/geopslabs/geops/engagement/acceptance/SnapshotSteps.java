package com.geopslabs.geops.engagement.acceptance;

import com.geopslabs.geops.engagement.domain.models.BusinessSnapshot;
import com.geopslabs.geops.engagement.domain.models.OfferSnapshot;
import com.geopslabs.geops.engagement.domain.models.RedeemedReservation;
import com.geopslabs.geops.engagement.domain.ports.BusinessSnapshotRepositoryPort;
import com.geopslabs.geops.engagement.domain.ports.OfferSnapshotRepositoryPort;
import com.geopslabs.geops.engagement.domain.ports.RedeemedReservationRepositoryPort;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class SnapshotSteps {
    private static final String CLEAN_TABLES = """
            TRUNCATE TABLE review_replies, reviews, saved_offers, saved_businesses, follows, redeemed_reservations,
                offer_snapshots, business_snapshots RESTART IDENTITY CASCADE""";
    private static final LocalDateTime REDEEMED_AT =
            LocalDateTime.ofInstant(AcceptanceTestConfiguration.ACCEPTANCE_NOW, ZoneOffset.UTC).minusDays(1);

    private final JdbcTemplate jdbcTemplate;
    private final BusinessSnapshotRepositoryPort businesses;
    private final OfferSnapshotRepositoryPort offers;
    private final RedeemedReservationRepositoryPort redemptions;

    public SnapshotSteps(JdbcTemplate jdbcTemplate, BusinessSnapshotRepositoryPort businesses,
                         OfferSnapshotRepositoryPort offers, RedeemedReservationRepositoryPort redemptions) {
        this.jdbcTemplate = jdbcTemplate;
        this.businesses = businesses;
        this.offers = offers;
        this.redemptions = redemptions;
    }

    @Before
    public void startWithEmptyTables() {
        jdbcTemplate.execute(CLEAN_TABLES);
    }

    @Given("business {long} is called {string}")
    public void businessIsCalled(Long businessId, String businessName) {
        businesses.upsert(new BusinessSnapshot(businessId, businessName));
    }

    @Given("offer {long} of business {long} titled {string} is published until {string}")
    public void offerIsPublished(Long offerId, Long businessId, String title, String validTo) {
        offers.upsert(new OfferSnapshot(offerId, businessId, title, LocalDate.parse(validTo),
                OfferSnapshot.PUBLISHED));
    }

    @Given("offer {long} is now valid until {string}")
    public void offerIsNowValidUntil(Long offerId, String validTo) {
        var offer = offers.findById(offerId).orElseThrow();
        offers.upsert(new OfferSnapshot(offer.offerId(), offer.businessId(), offer.title(), LocalDate.parse(validTo),
                offer.status()));
    }

    @Given("consumer {long} redeemed reservation {long} at business {long}")
    public void consumerRedeemedReservation(Long consumerId, Long reservationId, Long businessId) {
        redemptions.saveIfAbsent(new RedeemedReservation(reservationId, consumerId, businessId, REDEEMED_AT));
    }
}
