package com.geopslabs.geops.catalog.domain.models;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class OfferTest {
    private static final LocalDate TODAY = LocalDate.parse("2026-10-08");
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);
    private static final LocalDate NEXT_WEEK = TODAY.plusWeeks(1);
    private static final Long CAMPAIGN_ID = 14L;
    private static final Long BUSINESS_ID = 1L;

    @Test
    void publishedOfferIsValidBeforeItsLastDay() {
        assertThat(offer(OfferStatus.PUBLISHED, NEXT_WEEK).isValidOn(TODAY)).isTrue();
    }

    @Test
    void publishedOfferIsStillValidOnItsLastDay() {
        assertThat(offer(OfferStatus.PUBLISHED, TODAY).isValidOn(TODAY)).isTrue();
    }

    @Test
    void publishedOfferIsNotValidAfterItsLastDay() {
        assertThat(offer(OfferStatus.PUBLISHED, YESTERDAY).isValidOn(TODAY)).isFalse();
    }

    @Test
    void expiredOfferIsNotValid() {
        assertThat(offer(OfferStatus.EXPIRED, NEXT_WEEK).isValidOn(TODAY)).isFalse();
    }

    @Test
    void removedOfferIsNotValid() {
        assertThat(offer(OfferStatus.REMOVED, NEXT_WEEK).isValidOn(TODAY)).isFalse();
    }

    @Test
    void validAffiliatedOfferIsReservable() {
        assertThat(offer(OfferStatus.PUBLISHED, NEXT_WEEK).isReservableOn(TODAY)).isTrue();
    }

    @Test
    void publicSourceOfferIsNotReservable() {
        var offer = new Offer(1L, null, null, "Menú del día", "Solo en el local", Money.soles(BigDecimal.TEN),
                NEXT_WEEK, "Gastronomía", GeocodingStatus.GEOCODED, "Av. Grau 120, La Victoria", null,
                OfferSource.PUBLIC_SOURCE, "Diario El Barrio", OfferStatus.PUBLISHED, null);

        assertThat(offer.isValidOn(TODAY)).isTrue();
        assertThat(offer.isReservableOn(TODAY)).isFalse();
    }

    private static Offer offer(OfferStatus status, LocalDate validTo) {
        return new Offer(1L, CAMPAIGN_ID, BUSINESS_ID, "Menú ejecutivo a mitad de precio", "Lunes a viernes",
                Money.soles(BigDecimal.TEN), validTo, "Gastronomía", GeocodingStatus.GEOCODED,
                "Jr. Huánuco 1250, La Victoria", null, OfferSource.AFFILIATED, null, status, null);
    }
}
