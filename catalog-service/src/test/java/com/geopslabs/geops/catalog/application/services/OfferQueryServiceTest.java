package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import com.geopslabs.geops.catalog.domain.models.exceptions.OfferNotFoundException;
import com.geopslabs.geops.catalog.domain.models.queries.GetOfferByIdQuery;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferQueryServiceTest {
    private static final Long OFFER_ID = 1052L;
    private static final Long CAMPAIGN_ID = 14L;
    private static final Long BUSINESS_ID = 1L;
    private static final LocalDate VALID_TO = LocalDate.parse("2026-10-31");
    private static final BigDecimal PRICE = new BigDecimal("12.50");

    @Mock
    private OfferRepositoryPort offerRepository;

    private OfferQueryService service;

    @BeforeEach
    void setUp() {
        service = new OfferQueryService(offerRepository);
    }

    @Test
    void returnsTheOfferOfTheId() {
        var offer = offer();
        when(offerRepository.findById(OFFER_ID)).thenReturn(Optional.of(offer));

        assertThat(service.getById(new GetOfferByIdQuery(OFFER_ID))).isSameAs(offer);
    }

    @Test
    void unknownIdIsNotFound() {
        when(offerRepository.findById(OFFER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(new GetOfferByIdQuery(OFFER_ID)))
                .isInstanceOf(OfferNotFoundException.class)
                .hasMessage("Offer 1052 was not found");
    }

    private static Offer offer() {
        return new Offer(OFFER_ID, CAMPAIGN_ID, BUSINESS_ID, "Menú ejecutivo a mitad de precio",
                "De lunes a viernes de 12:00 a 15:00", Money.soles(PRICE), VALID_TO, "Gastronomía",
                GeocodingStatus.GEOCODED, "Jr. Huánuco 1250, La Victoria", null, OfferSource.AFFILIATED, null,
                OfferStatus.PUBLISHED, null);
    }
}
