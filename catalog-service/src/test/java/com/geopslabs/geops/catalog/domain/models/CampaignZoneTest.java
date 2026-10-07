package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidCampaignZoneException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampaignZoneTest {
    private static final GeoPoint STORE = new GeoPoint(-12.1211, -77.0297);
    private static final double METERS_PER_DEGREE = Math.toRadians(1) * GeoPoint.EARTH_MEAN_RADIUS_METERS;
    private static final int RADIUS_METERS = 800;
    private static final int BORDER_MARGIN_METERS = 1;
    private static final String DISTRICT = "Miraflores";

    @ParameterizedTest
    @ValueSource(ints = {CampaignZone.MIN_RADIUS_METERS, 800, CampaignZone.MAX_RADIUS_METERS})
    void radiusInsideTheLimitsIsAccepted(int radiusMeters) {
        var zone = CampaignZone.radius(STORE, radiusMeters);

        assertThat(zone.type()).isEqualTo(ZoneType.RADIUS);
        assertThat(zone.center()).isEqualTo(STORE);
        assertThat(zone.radiusMeters()).isEqualTo(radiusMeters);
    }

    @ParameterizedTest
    @ValueSource(ints = {CampaignZone.MIN_RADIUS_METERS - 1, CampaignZone.MAX_RADIUS_METERS + 1})
    void radiusOutsideTheLimitsIsRejected(int radiusMeters) {
        assertThatThrownBy(() -> CampaignZone.radius(STORE, radiusMeters))
                .isInstanceOf(InvalidCampaignZoneException.class);
    }

    @Test
    void radiusZoneWithoutCenterIsRejected() {
        assertThatThrownBy(() -> CampaignZone.radius(null, CampaignZone.MIN_RADIUS_METERS))
                .isInstanceOf(InvalidCampaignZoneException.class);
    }

    @Test
    void radiusZoneWithoutRadiusIsRejected() {
        assertThatThrownBy(() -> CampaignZone.radius(STORE, null)).isInstanceOf(InvalidCampaignZoneException.class);
    }

    @Test
    void districtKeepsItsNameAndCenterWithoutRadius() {
        var zone = CampaignZone.district(" Miraflores ", STORE);

        assertThat(zone.type()).isEqualTo(ZoneType.DISTRICT);
        assertThat(zone.district()).isEqualTo(DISTRICT);
        assertThat(zone.center()).isEqualTo(STORE);
        assertThat(zone.radiusMeters()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void districtWithoutNameIsRejected(String name) {
        assertThatThrownBy(() -> CampaignZone.district(name, STORE))
                .isInstanceOf(InvalidCampaignZoneException.class)
                .hasMessage("La zona por distrito necesita el nombre del distrito y su centro.");
    }

    @Test
    void districtWithoutCenterIsRejected() {
        assertThatThrownBy(() -> CampaignZone.district(DISTRICT, null))
                .isInstanceOf(InvalidCampaignZoneException.class);
    }

    @Test
    void radiusZoneCoversUpToItsBorder() {
        var zone = CampaignZone.radius(STORE, RADIUS_METERS);

        assertThat(zone.covers(north(RADIUS_METERS - BORDER_MARGIN_METERS))).isTrue();
        assertThat(zone.covers(north(RADIUS_METERS + BORDER_MARGIN_METERS))).isFalse();
    }

    @Test
    void districtZoneCoversTheDistrictCoverageAroundItsCenter() {
        var zone = CampaignZone.district(DISTRICT, STORE);

        assertThat(zone.covers(north(CampaignZone.DISTRICT_COVERAGE_METERS - BORDER_MARGIN_METERS))).isTrue();
        assertThat(zone.covers(north(CampaignZone.DISTRICT_COVERAGE_METERS + BORDER_MARGIN_METERS))).isFalse();
    }

    @Test
    void zoneSavedWithoutCenterCoversEveryPoint() {
        var legacy = new CampaignZone(ZoneType.RADIUS, null, RADIUS_METERS, null);

        assertThat(legacy.covers(north(CampaignZone.MAX_RADIUS_METERS * 2))).isTrue();
    }

    private static GeoPoint north(double meters) {
        return new GeoPoint(STORE.latitude() + meters / METERS_PER_DEGREE, STORE.longitude());
    }
}
