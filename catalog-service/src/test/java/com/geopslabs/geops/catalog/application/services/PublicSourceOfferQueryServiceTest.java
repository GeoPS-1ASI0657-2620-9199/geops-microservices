package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.LoadPublicOffersCommand;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.PublicSourceOffer;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidCategoryException;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidSearchRadiusException;
import com.geopslabs.geops.catalog.domain.models.queries.SearchNearbyOffersQuery;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PublicSourceOfferQueryServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneId.of("America/Lima"));
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);
    private static final GeoPoint ORIGIN = new GeoPoint(-12.0464, -77.0306);
    private static final double METERS_PER_DEGREE = Math.toRadians(1) * GeoPoint.EARTH_MEAN_RADIUS_METERS;

    private OfferRepositoryPort offers;
    private PublicSourceOfferQueryService service;

    @BeforeEach
    void setUp() {
        offers = mock(OfferRepositoryPort.class);
        service = new PublicSourceOfferQueryService(offers, CLOCK);
    }

    @Test
    void listsThePublicSourceOffersByDistanceWithTheirSource() {
        when(offers.findPublicSourceWithin(ORIGIN, 800, TODAY)).thenReturn(List.of(
                offer(2L, "Clases de marinera", 600, "Cultura y entretenimiento"),
                offer(1L, "Feria de libros usados", 300, "Librería y útiles")));

        var page = service.searchPublicSources(query(10, null));

        assertThat(page.content()).extracting(ranked -> ranked.offer().title())
                .containsExactly("Feria de libros usados", "Clases de marinera");
        assertThat(page.content().get(0).offer().sourceName()).isEqualTo("Municipalidad de Lima");
        assertThat(page.content().get(0).walkMinutes()).isEqualTo(4);
    }

    @Test
    void filtersByCategoryAndExactRadius() {
        when(offers.findPublicSourceWithin(ORIGIN, 400, TODAY)).thenReturn(List.of(
                offer(1L, "Feria de libros usados", 300, "Librería y útiles"),
                offer(2L, "Clases de marinera", 350, "Cultura y entretenimiento"),
                offer(3L, "Feria lejana", 450, "Librería y útiles")));

        var page = service.searchPublicSources(query(5, "libreria y utiles"));

        assertThat(page.content()).extracting(ranked -> ranked.offer().offerId()).containsExactly(1L);
    }

    @Test
    void rejectsTheSameParametersAsTheMainSearch() {
        assertThatThrownBy(() -> service.searchPublicSources(query(25, null)))
                .isInstanceOf(InvalidSearchRadiusException.class);
        assertThatThrownBy(() -> service.searchPublicSources(query(10, "Juguetería")))
                .isInstanceOf(InvalidCategoryException.class);
        verifyNoInteractions(offers);
    }

    @Test
    void loadsTheBatchAsPendingOffersWithoutCoordinates() {
        when(offers.save(any())).thenAnswer(call -> {
            Offer offer = call.getArgument(0);
            return new Offer(9001L, null, null, offer.getTitle(), offer.getConditions(), offer.getPrice(),
                    offer.getValidTo(), offer.getCategory(), offer.getGeocodingStatus(), offer.getAddress(),
                    offer.getImageUrl(), offer.getSource(), offer.getSourceName(), offer.getStatus(), null);
        });
        var loader = new PublicOfferCommandService(offers);

        var ids = loader.load(new LoadPublicOffersCommand(List.of(publicOffer("libreria y utiles"))));

        assertThat(ids).containsExactly(9001L);
        var saved = ArgumentCaptor.forClass(Offer.class);
        verify(offers).save(saved.capture());
        assertThat(saved.getValue().getSource()).isEqualTo(OfferSource.PUBLIC_SOURCE);
        assertThat(saved.getValue().getGeocodingStatus()).isEqualTo(GeocodingStatus.PENDING);
        assertThat(saved.getValue().getLocation()).isNull();
        assertThat(saved.getValue().getCampaignId()).isNull();
        assertThat(saved.getValue().getCategory()).isEqualTo("Librería y útiles");
    }

    @Test
    void aBatchWithAnUnknownCategorySavesNothing() {
        var loader = new PublicOfferCommandService(offers);
        var batch = new LoadPublicOffersCommand(List.of(publicOffer("Librería y útiles"), publicOffer("Juguetería")));

        assertThatThrownBy(() -> loader.load(batch)).isInstanceOf(InvalidCategoryException.class);
        verify(offers, never()).save(any());
    }

    private static LoadPublicOffersCommand.PublicOffer publicOffer(String category) {
        return new LoadPublicOffersCommand.PublicOffer("Feria de libros usados", "Entrada libre", BigDecimal.ZERO,
                LocalDate.of(2026, 10, 30), category, "Jr. de la Unión 300, Cercado de Lima", "Municipalidad de Lima",
                null);
    }

    private static SearchNearbyOffersQuery query(int minutes, String category) {
        return new SearchNearbyOffersQuery(ORIGIN.latitude(), ORIGIN.longitude(), minutes, category, 0, 20);
    }

    private static PublicSourceOffer offer(Long id, String title, int metersNorth, String category) {
        return new PublicSourceOffer(id, title, "Municipalidad de Lima", BigDecimal.ZERO, TODAY.plusDays(20), category,
                "Jr. de la Unión 300", null,
                new GeoPoint(ORIGIN.latitude() + metersNorth / METERS_PER_DEGREE, ORIGIN.longitude()));
    }
}
