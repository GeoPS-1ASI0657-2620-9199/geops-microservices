package com.geopslabs.geops.catalog.domain.models;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeohashCoverTest {
    private static final GeoPoint MIRAFLORES = new GeoPoint(-12.1211, -77.0297);
    private static final int TWENTY_MINUTES = 1_600;
    private static final double METERS_PER_DEGREE = Math.toRadians(1) * GeoPoint.EARTH_MEAN_RADIUS_METERS;

    @Test
    void encodesTheStandardGeohash() {
        // Reference value of the geohash definition (57.64911, 10.40744) -> u4pruydqqvj.
        assertThat(GeoCell.containing(new GeoPoint(57.64911, 10.40744)).geohash()).isEqualTo("u4pruy");
    }

    @Test
    void decodesTheCellThatContainsThePoint() {
        var cell = GeoCell.containing(MIRAFLORES);
        var bounds = cell.bounds();

        assertThat(MIRAFLORES.latitude()).isBetween(bounds.minLatitude(), bounds.maxLatitude());
        assertThat(MIRAFLORES.longitude()).isBetween(bounds.minLongitude(), bounds.maxLongitude());
        assertThat(cell.distanceFrom(MIRAFLORES)).isZero();
    }

    @Test
    void theCircleOfTwentyMinutesIsCovered() {
        var cells = GeohashCover.cover(MIRAFLORES, TWENTY_MINUTES);

        everyPointOfTheCircle(TWENTY_MINUTES).forEach(point ->
                assertThat(cells).contains(GeoCell.containing(point)));
        assertThat(cells).hasSizeBetween(4, 20);
    }

    @Test
    void noCellIsOutsideTheCircle() {
        var cells = GeohashCover.cover(MIRAFLORES, TWENTY_MINUTES);

        assertThat(cells).allSatisfy(cell -> assertThat(cell.distanceFrom(MIRAFLORES)).isLessThanOrEqualTo(TWENTY_MINUTES));
    }

    @Test
    void aSmallRadiusInsideOneCellReadsOnlyThatCell() {
        var center = GeoCell.containing(MIRAFLORES).bounds();
        var middle = new GeoPoint((center.minLatitude() + center.maxLatitude()) / 2,
                (center.minLongitude() + center.maxLongitude()) / 2);

        assertThat(GeohashCover.cover(middle, 50)).containsExactly(GeoCell.containing(middle));
    }

    @Test
    void theEnvelopeContainsEveryCell() {
        var cells = GeohashCover.cover(MIRAFLORES, TWENTY_MINUTES);
        var envelope = GeoCell.envelopeOf(cells);

        cells.forEach(cell -> {
            var bounds = cell.bounds();
            assertThat(envelope.center().distanceTo(new GeoPoint(bounds.minLatitude(), bounds.minLongitude())))
                    .isLessThanOrEqualTo(envelope.radiusMeters());
            assertThat(envelope.center().distanceTo(new GeoPoint(bounds.maxLatitude(), bounds.maxLongitude())))
                    .isLessThanOrEqualTo(envelope.radiusMeters());
        });
    }

    @Test
    void rejectsAGeohashOfAnotherLength() {
        assertThatThrownBy(() -> new GeoCell("6mc5")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeoCell("6mc5aa")).isInstanceOf(IllegalArgumentException.class);
    }

    private static java.util.stream.Stream<GeoPoint> everyPointOfTheCircle(int radius) {
        var longitudeMeters = METERS_PER_DEGREE * Math.cos(Math.toRadians(MIRAFLORES.latitude()));
        return IntStream.range(0, 360).boxed().flatMap(degrees -> IntStream.of(radius - 1, radius / 2, 0)
                .mapToObj(meters -> new GeoPoint(
                        MIRAFLORES.latitude() + meters * Math.cos(Math.toRadians(degrees)) / METERS_PER_DEGREE,
                        MIRAFLORES.longitude() + meters * Math.sin(Math.toRadians(degrees)) / longitudeMeters)));
    }
}
