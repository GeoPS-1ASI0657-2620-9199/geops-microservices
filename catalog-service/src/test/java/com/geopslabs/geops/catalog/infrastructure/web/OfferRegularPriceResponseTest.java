package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.NearbyOffersPage;
import com.geopslabs.geops.catalog.application.usecases.OfferDetail;
import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.NearbyOfferCandidate;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import com.geopslabs.geops.catalog.domain.models.RankedOffer;
import com.geopslabs.geops.catalog.domain.models.ZoneType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OfferRegularPriceResponseTest {
    private static final GeoPoint STORE = new GeoPoint(-12.1211, -77.0297);
    private static final LocalDate VALID_TO = LocalDate.parse("2026-10-15");
    private static final BigDecimal PRICE = new BigDecimal("15.00");
    private static final BigDecimal REGULAR_PRICE = new BigDecimal("30.00");
    private static final double DISTANCE_METERS = 350.0;

    @Test
    void theCampaignOfferAnswersItsRegularPrice() {
        assertThat(OfferResponseAssembler.toResponse(offer(Money.soles(REGULAR_PRICE))).regularPrice())
                .isEqualTo(REGULAR_PRICE);
    }

    @Test
    void anOfferWithoutRegularPriceAnswersNull() {
        assertThat(OfferResponseAssembler.toResponse(offer(null)).regularPrice()).isNull();
        assertThat(OfferDetailResponseAssembler.toResponse(new OfferDetail(offer(null), "Don Pepe", false, true))
                .regularPrice()).isNull();
    }

    @Test
    void theOfferDetailAnswersItsRegularPrice() {
        var detail = new OfferDetail(offer(Money.soles(REGULAR_PRICE)), "Don Pepe", false, true);

        var response = OfferDetailResponseAssembler.toResponse(detail);

        assertThat(response.price()).isEqualTo(PRICE);
        assertThat(response.regularPrice()).isEqualTo(REGULAR_PRICE);
    }

    @Test
    void theNearbySearchAnswersTheRegularPriceOfEachOffer() {
        var candidate = new NearbyOfferCandidate(1052L, "2x1 en almuerzos", PRICE, REGULAR_PRICE, VALID_TO,
                "Gastronomía", "Av. Larco 345, Miraflores", null, STORE, 84L, "Don Pepe", true, 0,
                CampaignZone.radius(STORE, CampaignZone.MAX_RADIUS_METERS));
        var page = new NearbyOffersPage(List.of(RankedOffer.of(candidate, DISTANCE_METERS)), 0, 1, 1);

        var response = NearbyOffersResponseAssembler.toResponse(page);

        assertThat(response.content()).singleElement().satisfies(offer -> {
            assertThat(offer.price()).isEqualTo(PRICE);
            assertThat(offer.regularPrice()).isEqualTo(REGULAR_PRICE);
        });
    }

    private static Offer offer(Money regularPrice) {
        return new Offer(1052L, 14L, 84L, "2x1 en almuerzos", "Lunes a viernes", Money.soles(PRICE), regularPrice,
                VALID_TO, "Gastronomía", GeocodingStatus.GEOCODED, "Av. Larco 345, Miraflores", null,
                OfferSource.AFFILIATED, null, OfferStatus.PUBLISHED, STORE);
    }
}
