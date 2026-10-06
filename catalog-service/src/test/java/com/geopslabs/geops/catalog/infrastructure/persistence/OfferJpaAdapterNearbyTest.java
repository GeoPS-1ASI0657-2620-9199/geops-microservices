package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.acceptance.CucumberSpringConfiguration;
import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.CampaignStatus;
import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.DateRange;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    private Long activeCampaignId;
    private Long pausedCampaignId;

    @BeforeEach
    void startWithOneBusinessAndTwoCampaigns() {
        offerRows.deleteAll();
        campaignRows.deleteAll();
        standingRows.deleteAll();
        standings.save(new MerchantStanding(BUSINESS_ID, BUSINESS_NAME, true, 0, FULL_COMPLIANCE, true,
                LocalDateTime.now()));
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

    private Long campaign(String name, CampaignStatus status) {
        return campaigns.save(new Campaign(null, BUSINESS_ID, name, name, OCTOBER,
                new CampaignZone(ZoneType.RADIUS, RADIUS_METERS, null), status, Money.soles(PRICE))).getId();
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
