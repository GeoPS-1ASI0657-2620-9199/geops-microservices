package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.GetOfferDetailUseCase;
import com.geopslabs.geops.catalog.application.usecases.OfferDetail;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import com.geopslabs.geops.catalog.domain.models.queries.GetOfferDetailQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OffersControllerTest {
    private static final Long OFFER_ID = 1052L;

    private GetOfferDetailUseCase getOfferDetail;
    private OffersController controller;

    @BeforeEach
    void setUp() {
        getOfferDetail = mock(GetOfferDetailUseCase.class);
        controller = new OffersController(getOfferDetail);
        when(getOfferDetail.getDetail(any())).thenReturn(new OfferDetail(offer(), "Restaurante Don Pepe", false, true));
    }

    @Test
    void trackClickAsksToCountTheClick() {
        controller.getOffer(OFFER_ID, "click");

        verify(getOfferDetail).getDetail(new GetOfferDetailQuery(OFFER_ID, true));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "view", "CLICK"})
    void withoutTrackClickTheDetailIsReadWithoutCounting(String track) {
        controller.getOffer(OFFER_ID, track);

        verify(getOfferDetail).getDetail(new GetOfferDetailQuery(OFFER_ID, false));
    }

    private static Offer offer() {
        return new Offer(OFFER_ID, 31L, 84L, "2x1 en almuerzos ejecutivos", "Un cupón por mesa.",
                Money.soles(new BigDecimal("15.00")), LocalDate.parse("2026-10-15"), "Gastronomía",
                GeocodingStatus.GEOCODED, "Av. Larco 345, Miraflores", null, OfferSource.AFFILIATED, null,
                OfferStatus.PUBLISHED, new GeoPoint(-12.1211, -77.0297));
    }
}
