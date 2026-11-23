package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidSearchTextException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchTextTest {
    private static final GeoPoint ORIGIN = new GeoPoint(-12.1211, -77.0297);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void aMissingOrBlankTextMeansNoFilter(String raw) {
        assertThat(SearchText.parseOrNull(raw)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "  b  "})
    void aTextOfOneCharacterIsRejected(String raw) {
        assertThatThrownBy(() -> SearchText.parseOrNull(raw))
                .isInstanceOf(InvalidSearchTextException.class)
                .extracting("code").isEqualTo("INVALID_REQUEST");
    }

    @Test
    void sixtyCharactersAreAcceptedAndSixtyOneRejected() {
        assertThat(SearchText.parseOrNull("x".repeat(60))).isNotNull();
        assertThatThrownBy(() -> SearchText.parseOrNull("x".repeat(61)))
                .isInstanceOf(InvalidSearchTextException.class);
    }

    @Test
    void theLengthIsMeasuredAfterTrimming() {
        assertThat(SearchText.parseOrNull("   " + "x".repeat(60) + "   ")).isNotNull();
    }

    @Test
    void matchesTheTitleIgnoringCaseAndAccents() {
        var text = SearchText.parseOrNull("CAFE PASADO");

        assertThat(text.matches(candidate("Café  pasado del día", "Comercio", "Gastronomía"))).isTrue();
        assertThat(text.matches(candidate("Té verde", "Comercio", "Gastronomía"))).isFalse();
    }

    @Test
    void matchesTheBusinessName() {
        assertThat(SearchText.parseOrNull("don pepe").matches(candidate("2x1", "Restaurante Don Pépe", "Gastronomía")))
                .isTrue();
    }

    @Test
    void matchesTheCategory() {
        assertThat(SearchText.parseOrNull("panaderia")
                .matches(candidate("Promo", "Comercio", "Cafetería y panadería"))).isTrue();
    }

    @Test
    void aMissingBusinessNameDoesNotBreakTheMatch() {
        assertThat(SearchText.parseOrNull("menu").matches(candidate("Menú ejecutivo", null, "Gastronomía"))).isTrue();
        assertThat(SearchText.parseOrNull("zzz").matches(candidate("Menú ejecutivo", null, "Gastronomía"))).isFalse();
    }

    private static NearbyOfferCandidate candidate(String title, String businessName, String category) {
        return new NearbyOfferCandidate(1L, title, BigDecimal.TEN, null, LocalDate.of(2026, 10, 15), category,
                "Av. Larco 345", null, ORIGIN, 84L, businessName, false, 0,
                CampaignZone.radius(ORIGIN, CampaignZone.MAX_RADIUS_METERS));
    }
}
