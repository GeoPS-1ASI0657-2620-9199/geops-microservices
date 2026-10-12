package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.acceptance.CucumberSpringConfiguration;
import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.CampaignStatus;
import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.DateRange;
import com.geopslabs.geops.catalog.domain.models.GeoCell;
import com.geopslabs.geops.catalog.domain.models.GeohashCover;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.NearbyOfferCandidate;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import com.geopslabs.geops.catalog.domain.models.ZoneType;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class OfferJpaAdapterNearbyTest extends CucumberSpringConfiguration {
    private static final GeoPoint ORIGIN = new GeoPoint(-12.1211, -77.0297);
    private static final double METERS_PER_DEGREE = Math.toRadians(1) * GeoPoint.EARTH_MEAN_RADIUS_METERS;
    private static final int RADIUS_METERS = 800;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);
    private static final DateRange OCTOBER = new DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    private static final LocalDate VALID_TO = LocalDate.of(2026, 10, 15);
    private static final LocalDate EXPIRED = LocalDate.of(2026, 10, 5);
    private static final long BUSINESS_ID = 84L;
    private static final String BUSINESS_NAME = "Cevichería Doña Rosa";
    private static final BigDecimal PRICE = new BigDecimal("15.00");
    private static final BigDecimal FULL_COMPLIANCE = new BigDecimal("100.00");
    private static final double SAME_METERS = 0.5;
    private static final String DISTRICT = "Miraflores";
    private static final int DISTRICT_CENTER_METERS_EAST = 500;

    @Autowired
    private OfferRepositoryPort offers;
    @Autowired
    private CampaignRepositoryPort campaigns;
    @Autowired
    private MerchantStandingRepositoryPort standings;
    @Autowired
    private OfferJpaRepository offerRows;
    @Autowired
    private CampaignJpaRepository campaignRows;
    @Autowired
    private MerchantStandingJpaRepository standingRows;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private org.springframework.transaction.support.TransactionTemplate transactions;

    private Long activeCampaignId;
    private Long pausedCampaignId;

    @BeforeEach
    void startWithOneBusinessAndTwoCampaigns() {
        jdbc.update("DELETE FROM standing_changes");
        offerRows.deleteAll();
        campaignRows.deleteAll();
        standingRows.deleteAll();
        standings.save(new MerchantStanding(BUSINESS_ID, BUSINESS_NAME, true, 0, FULL_COMPLIANCE, true,
                Instant.now()));
        activeCampaignId = campaign("Almuerzos de octubre", CampaignStatus.ACTIVE);
        pausedCampaignId = campaign("Campaña en pausa", CampaignStatus.PAUSED);
    }

    @Test
    void returnsOnlyValidOffersOfActiveCampaignsInsideTheRadius() {
        offer("Vigente a 300 m", activeCampaignId, VALID_TO, north(300));
        offer("Vencida a 300 m", activeCampaignId, EXPIRED, north(300));
        offer("Campaña pausada a 300 m", pausedCampaignId, VALID_TO, north(300));
        offer("Vigente a 900 m", activeCampaignId, VALID_TO, north(900));

        var candidates = offers.findPublishedWithin(ORIGIN, RADIUS_METERS, TODAY);

        assertThat(candidates).extracting(NearbyOfferCandidate::title).containsExactly("Vigente a 300 m");
        var candidate = candidates.get(0);
        assertThat(candidate.businessName()).isEqualTo(BUSINESS_NAME);
        assertThat(candidate.verifiedSeal()).isTrue();
        assertThat(ORIGIN.distanceTo(candidate.location())).isCloseTo(300, within(SAME_METERS));
    }

    @Test
    void keepsLongitudeAndLatitudeInTheirPlaces() {
        offer("Vigente a 300 m al este", activeCampaignId, VALID_TO, east(300));

        var candidates = offers.findPublishedWithin(ORIGIN, RADIUS_METERS, TODAY);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).location().longitude()).isGreaterThan(ORIGIN.longitude());
        assertThat(candidates.get(0).location().latitude()).isCloseTo(ORIGIN.latitude(), within(1e-9));
    }

    @Test
    void readsTheCampaignZoneWithItsCenter() {
        var districtCenter = east(DISTRICT_CENTER_METERS_EAST);
        var campaignId = campaigns.save(new Campaign(null, BUSINESS_ID, DISTRICT, DISTRICT, OCTOBER,
                CampaignZone.district(DISTRICT, districtCenter), CampaignStatus.ACTIVE, Money.soles(PRICE))).getId();
        offer("Vigente en el distrito", campaignId, VALID_TO, north(300));

        var zone = offers.findPublishedWithin(ORIGIN, RADIUS_METERS, TODAY).get(0).zone();

        assertThat(zone.type()).isEqualTo(ZoneType.DISTRICT);
        assertThat(zone.district()).isEqualTo(DISTRICT);
        assertThat(zone.radiusMeters()).isNull();
        assertThat(zone.center().latitude()).isCloseTo(districtCenter.latitude(), within(1e-9));
        assertThat(zone.center().longitude()).isCloseTo(districtCenter.longitude(), within(1e-9));
    }

    @Test
    void readsACampaignSavedWithoutZoneCenter() {
        offer("Vigente a 300 m", activeCampaignId, VALID_TO, north(300));

        var zone = offers.findPublishedWithin(ORIGIN, RADIUS_METERS, TODAY).get(0).zone();

        assertThat(zone.type()).isEqualTo(ZoneType.RADIUS);
        assertThat(zone.center()).isNull();
        assertThat(zone.radiusMeters()).isEqualTo(RADIUS_METERS);
    }

    @Test
    void groupsTheOffersByTheSameCellTheServiceComputes() {
        IntStream.range(0, 24).forEach(step -> offer("Oferta " + step, activeCampaignId, VALID_TO,
                around(step * 15, 100 + step * 60)));
        offer("Vencida", activeCampaignId, EXPIRED, north(100));
        var cells = GeohashCover.cover(ORIGIN, 1_600);

        var byCell = offers.findPublishedInCells(cells, TODAY);

        var all = byCell.values().stream().flatMap(java.util.List::stream).toList();
        assertThat(all).hasSize(24).noneMatch(candidate -> candidate.title().equals("Vencida"));
        byCell.forEach((cell, inCell) -> inCell.forEach(candidate ->
                assertThat(GeoCell.containing(candidate.location()).geohash()).isEqualTo(cell)));
        assertThat(byCell.keySet()).isSubsetOf(cells.stream().map(GeoCell::geohash).collect(Collectors.toSet()));
    }

    @Test
    void readsOnlyTheCellsAskedFor() {
        offer("Aquí", activeCampaignId, VALID_TO, north(10));
        offer("Lejos", activeCampaignId, VALID_TO, north(1_500));
        var here = GeoCell.containing(north(10));

        var byCell = offers.findPublishedInCells(java.util.Set.of(here), TODAY);

        assertThat(byCell).containsOnlyKeys(here.geohash());
        assertThat(byCell.get(here.geohash())).extracting(NearbyOfferCandidate::title).containsExactly("Aquí");
    }

    @Test
    void expiresThePublishedOffersThatEndedAndReturnsWhereTheyWere() {
        offer("Vencida", activeCampaignId, EXPIRED, north(100));
        offer("Vigente", activeCampaignId, VALID_TO, north(200));

        var expired = transactions.execute(status -> offers.expirePublishedBefore(TODAY));

        assertThat(expired).singleElement().satisfies(point ->
                assertThat(ORIGIN.distanceTo(point)).isCloseTo(100, within(SAME_METERS)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM offers WHERE status = 'EXPIRED'", Integer.class))
                .isEqualTo(1);
        assertThat(offers.findPublishedLocationsOfBusiness(BUSINESS_ID, TODAY)).hasSize(1);
    }

    @Test
    void listsOnlyThePublicSourceOffersThatHaveCoordinates() {
        offers.save(publicOffer("Feria geocodificada", GeocodingStatus.GEOCODED, north(300)));
        offers.save(publicOffer("Feria pendiente", GeocodingStatus.PENDING, null));
        offer("Afiliada", activeCampaignId, VALID_TO, north(200));

        var found = offers.findPublicSourceWithin(ORIGIN, RADIUS_METERS, TODAY);

        assertThat(found).extracting(com.geopslabs.geops.catalog.domain.models.PublicSourceOffer::title)
                .containsExactly("Feria geocodificada");
        assertThat(found.get(0).sourceName()).isEqualTo("Municipalidad de Lima");
        assertThat(offers.findPublishedWithin(ORIGIN, RADIUS_METERS, TODAY)).extracting(NearbyOfferCandidate::title)
                .containsExactly("Afiliada");
    }

    private static Offer publicOffer(String title, GeocodingStatus status, GeoPoint location) {
        return new Offer(null, null, null, title, "Entrada libre", Money.soles(BigDecimal.ZERO), VALID_TO,
                "Librería y útiles", status, "Jr. de la Unión 300, Cercado de Lima", null, OfferSource.PUBLIC_SOURCE,
                "Municipalidad de Lima", OfferStatus.PUBLISHED, location);
    }

    private static GeoPoint around(int degrees, double meters) {
        var longitudeMeters = METERS_PER_DEGREE * Math.cos(Math.toRadians(ORIGIN.latitude()));
        return new GeoPoint(ORIGIN.latitude() + meters * Math.cos(Math.toRadians(degrees)) / METERS_PER_DEGREE,
                ORIGIN.longitude() + meters * Math.sin(Math.toRadians(degrees)) / longitudeMeters);
    }

    private Long campaign(String name, CampaignStatus status) {
        return campaigns.save(new Campaign(null, BUSINESS_ID, name, name, OCTOBER,
                new CampaignZone(ZoneType.RADIUS, null, RADIUS_METERS, null), status, Money.soles(PRICE))).getId();
    }

    private void offer(String title, Long campaignId, LocalDate validTo, GeoPoint location) {
        offers.save(new Offer(null, campaignId, BUSINESS_ID, title, "Un cupón por mesa", Money.soles(PRICE), validTo,
                "Gastronomía", GeocodingStatus.GEOCODED, "Av. Larco 345, Miraflores", null, OfferSource.AFFILIATED,
                null, OfferStatus.PUBLISHED, location));
    }

    private static GeoPoint north(double meters) {
        return new GeoPoint(ORIGIN.latitude() + meters / METERS_PER_DEGREE, ORIGIN.longitude());
    }

    private static GeoPoint east(double meters) {
        var degrees = meters / (METERS_PER_DEGREE * Math.cos(Math.toRadians(ORIGIN.latitude())));
        return new GeoPoint(ORIGIN.latitude(), ORIGIN.longitude() + degrees);
    }
}
