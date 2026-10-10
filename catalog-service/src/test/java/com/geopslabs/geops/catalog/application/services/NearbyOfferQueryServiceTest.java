package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.NearbyOfferCandidate;
import com.geopslabs.geops.catalog.domain.models.ProximitySearchService;
import com.geopslabs.geops.catalog.domain.models.RankedOffer;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidGeoPointException;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidSearchRadiusException;
import com.geopslabs.geops.catalog.domain.models.queries.SearchNearbyOffersQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NearbyOfferQueryServiceTest {
    private static final GeoPoint ORIGIN = new GeoPoint(-12.1211, -77.0297);

    private ProximitySearchService proximitySearch;
    private NearbyOfferQueryService service;

    @BeforeEach
    void setUp() {
        proximitySearch = mock(ProximitySearchService.class);
        service = new NearbyOfferQueryService(proximitySearch);
    }

    @Test
    void pagesFortyFiveOffersInThreePagesOfTwenty() {
        when(proximitySearch.search(ORIGIN, 800, null)).thenReturn(ranked(45));

        var last = service.searchNearbyOffers(new SearchNearbyOffersQuery(-12.1211, -77.0297, 10, 2, 20));

        assertThat(last.totalPages()).isEqualTo(3);
        assertThat(last.totalElements()).isEqualTo(45);
        assertThat(last.page()).isEqualTo(2);
        assertThat(last.content()).hasSize(5);
        assertThat(last.content().get(0).offer().offerId()).isEqualTo(41L);
    }

    @Test
    void answersAnEmptyPageWhenTheZoneHasNoOffers() {
        when(proximitySearch.search(any(), anyInt(), any())).thenReturn(List.of());

        var empty = service.searchNearbyOffers(new SearchNearbyOffersQuery(-12.1211, -77.0297, 10, 0, 20));

        assertThat(empty.content()).isEmpty();
        assertThat(empty.totalElements()).isZero();
        assertThat(empty.totalPages()).isZero();
    }

    @Test
    void answersAnEmptyPageAfterTheLastOne() {
        when(proximitySearch.search(any(), anyInt(), any())).thenReturn(ranked(5));

        var beyond = service.searchNearbyOffers(new SearchNearbyOffersQuery(-12.1211, -77.0297, 10, 3, 20));

        assertThat(beyond.content()).isEmpty();
        assertThat(beyond.totalPages()).isEqualTo(1);
    }

    @Test
    void rejectsARadiusOfFourMinutesBeforeSearching() {
        assertThatThrownBy(() -> service.searchNearbyOffers(new SearchNearbyOffersQuery(-12.1211, -77.0297, 4, 0, 20)))
                .isInstanceOf(InvalidSearchRadiusException.class);
        verifyNoInteractions(proximitySearch);
    }

    @Test
    void rejectsInvalidCoordinatesBeforeSearching() {
        assertThatThrownBy(() -> service.searchNearbyOffers(new SearchNearbyOffersQuery(95, -77.0297, 10, 0, 20)))
                .isInstanceOf(InvalidGeoPointException.class);
        verifyNoInteractions(proximitySearch);
    }

    @Test
    void rejectsANegativePageOrASizeOutsideOneToTwenty() {
        assertThatThrownBy(() -> service.searchNearbyOffers(new SearchNearbyOffersQuery(-12.1211, -77.0297, 10, -1, 20)))
                .isInstanceOf(InvalidPageRequestException.class);
        assertThatThrownBy(() -> service.searchNearbyOffers(new SearchNearbyOffersQuery(-12.1211, -77.0297, 10, 0, 21)))
                .isInstanceOf(InvalidPageRequestException.class);
        assertThatThrownBy(() -> service.searchNearbyOffers(new SearchNearbyOffersQuery(-12.1211, -77.0297, 10, 0, 0)))
                .isInstanceOf(InvalidPageRequestException.class);
    }

    private static List<RankedOffer> ranked(int count) {
        return LongStream.rangeClosed(1, count)
                .mapToObj(id -> RankedOffer.of(new NearbyOfferCandidate(id, "Oferta " + id, BigDecimal.TEN,
                        LocalDate.of(2026, 10, 15), "Gastronomía", "Av. Larco 345, Miraflores", null, ORIGIN, 84L, "Comercio", false, 0,
                        CampaignZone.radius(ORIGIN, CampaignZone.MAX_RADIUS_METERS)), id * 10.0))
                .toList();
    }
}
