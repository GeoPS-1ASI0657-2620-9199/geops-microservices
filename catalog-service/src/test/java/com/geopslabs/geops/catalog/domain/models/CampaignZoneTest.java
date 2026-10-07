package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidCampaignZoneException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampaignZoneTest {
    private static final GeoPoint STORE = new GeoPoint(-12.1211, -77.0297);

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
}
