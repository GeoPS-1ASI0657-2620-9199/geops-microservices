package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProximitySearchServiceTest {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-05T02:00:00Z"), LIMA);
    private static final GeoPoint ORIGIN = new GeoPoint(-12.1211, -77.0297);
    private static final double METERS_PER_DEGREE = Math.toRadians(1) * GeoPoint.EARTH_MEAN_RADIUS_METERS;
    private static final CampaignZone WHOLE_AREA = CampaignZone.radius(ORIGIN, CampaignZone.MAX_RADIUS_METERS);
    private static final int SEARCH_RADIUS_METERS = 1_600;
    private static final int STORE_METERS_NORTH = 1_200;
    private static final int ZONE_RADIUS_METERS = 800;
    private static final int WIDE_ZONE_RADIUS_METERS = 1_500;

    private OfferRepositoryPort offers;
    private ProximitySearchService service;

    @BeforeEach
    void setUp() {
        offers = mock(OfferRepositoryPort.class);
        service = new ProximitySearchService(offers, CLOCK);
    }

    @Test
    void asksForTheCandidatesOfTodayInLima() {
        when(offers.findPublishedWithin(any(), anyInt(), any())).thenReturn(List.of());

        service.search(ORIGIN, 800, null);

        verify(offers).findPublishedWithin(ORIGIN, 800, LocalDate.of(2026, 10, 4));
    }

    @Test
    void ordersByDistanceWithMetersAndWalkingMinutes() {
        when(offers.findPublishedWithin(eq(ORIGIN), eq(800), any()))
                .thenReturn(List.of(candidate(1L, 600, false, 0), candidate(2L, 150, false, 0)));

        var result = service.search(ORIGIN, 800, null);

        assertThat(result).extracting(ranked -> ranked.offer().offerId()).containsExactly(2L, 1L);
        assertThat(result.get(0).distanceMeters()).isBetween(149.5, 150.5);
        assertThat(result.get(0).walkMinutes()).isEqualTo(2);
    }

    @Test
    void dropsTheCandidatesBeyondTheRadius() {
        when(offers.findPublishedWithin(any(), anyInt(), any()))
                .thenReturn(List.of(candidate(1L, 300, false, 0), candidate(2L, 900, false, 0)));

        var result = service.search(ORIGIN, 800, null);

        assertThat(result).extracting(ranked -> ranked.offer().offerId()).containsExactly(1L);
    }

    @Test
    void putsMerchantsWithoutOpenReportsFirstInsideTheSameBand() {
        when(offers.findPublishedWithin(any(), anyInt(), any())).thenReturn(List.of(
                candidate(210L, 210, true, 2),
                candidate(260L, 260, false, 0),
                candidate(150L, 150, false, 3)));

        var result = service.search(ORIGIN, 800, null);

        assertThat(result).extracting(ranked -> ranked.offer().offerId()).containsExactly(150L, 260L, 210L);
    }

    @Test
    void putsVerifiedMerchantsNextInsideTheSameBand() {
        when(offers.findPublishedWithin(any(), anyInt(), any()))
                .thenReturn(List.of(candidate(1L, 220, false, 0), candidate(2L, 280, true, 0)));

        var result = service.search(ORIGIN, 800, null);

        assertThat(result).extracting(ranked -> ranked.offer().offerId()).containsExactly(2L, 1L);
    }

    @Test
    void dropsTheOffersWhoseRadiusZoneDoesNotCoverTheConsumer() {
        var storeNorth = north(STORE_METERS_NORTH);
        when(offers.findPublishedWithin(any(), anyInt(), any())).thenReturn(List.of(
                candidate(1L, STORE_METERS_NORTH, CampaignZone.radius(storeNorth, ZONE_RADIUS_METERS)),
                candidate(2L, STORE_METERS_NORTH, CampaignZone.radius(storeNorth, WIDE_ZONE_RADIUS_METERS))));

        var result = service.search(ORIGIN, SEARCH_RADIUS_METERS, null);

        assertThat(result).extracting(ranked -> ranked.offer().offerId()).containsExactly(2L);
    }

    @Test
    void keepsTheOffersWhoseDistrictZoneCoversTheConsumer() {
        var districtCenter = north(STORE_METERS_NORTH);
        var farDistrictCenter = north(STORE_METERS_NORTH + CampaignZone.DISTRICT_COVERAGE_METERS);
        when(offers.findPublishedWithin(any(), anyInt(), any())).thenReturn(List.of(
                candidate(1L, STORE_METERS_NORTH, CampaignZone.district("Miraflores", districtCenter)),
                candidate(2L, STORE_METERS_NORTH, CampaignZone.district("San Isidro", farDistrictCenter))));

        var result = service.search(ORIGIN, SEARCH_RADIUS_METERS, null);

        assertThat(result).extracting(ranked -> ranked.offer().offerId()).containsExactly(1L);
    }

    private static NearbyOfferCandidate candidate(Long id, double metersNorth, boolean verified, int openReports) {
        return candidate(id, metersNorth, verified, openReports, WHOLE_AREA);
    }

    private static NearbyOfferCandidate candidate(Long id, double metersNorth, CampaignZone zone) {
        return candidate(id, metersNorth, false, 0, zone);
    }

    private static NearbyOfferCandidate candidate(Long id, double metersNorth, boolean verified, int openReports,
                                                  CampaignZone zone) {
        return new NearbyOfferCandidate(id, "Oferta " + id, new BigDecimal("15.00"), LocalDate.of(2026, 10, 15),
                "Gastronomía", north(metersNorth), 84L, "Cevichería Doña Rosa", verified, openReports, zone);
    }

    private static GeoPoint north(double meters) {
        return new GeoPoint(ORIGIN.latitude() + meters / METERS_PER_DEGREE, ORIGIN.longitude());
    }
}
