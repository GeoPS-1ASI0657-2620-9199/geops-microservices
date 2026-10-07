package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import com.geopslabs.geops.catalog.domain.models.exceptions.OfferNotFoundException;
import com.geopslabs.geops.catalog.domain.models.queries.GetOfferByIdQuery;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferQueryServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-08T13:05:00Z");
    private static final Instant LATE_NIGHT_IN_LIMA = Instant.parse("2026-10-09T03:00:00Z");
    private static final LocalDate TODAY_IN_LIMA = LocalDate.parse("2026-10-08");
    private static final Long OFFER_ID = 1052L;
    private static final Long CAMPAIGN_ID = 31L;
    private static final Long BUSINESS_ID = 84L;
    private static final String BUSINESS_NAME = "Restaurante Don Pepe";
    private static final String SOURCE_NAME = "Diario El Barrio";
    private static final BigDecimal PRICE = new BigDecimal("15.00");
    private static final BigDecimal FULL_COMPLIANCE = new BigDecimal("100.00");
    private static final GeoPoint STORE = new GeoPoint(-12.1211, -77.0297);

    @Mock
    private OfferRepositoryPort offerRepository;

    @Mock
    private MerchantStandingRepositoryPort merchantStandingRepository;

    private OfferQueryService service;

    @BeforeEach
    void setUp() {
        service = new OfferQueryService(offerRepository, merchantStandingRepository,
                Clock.fixed(NOW, OfferQueryService.OFFER_ZONE));
    }

    @Test
    void detailOfAValidOfferCarriesItsLocationAndMerchant() {
        var offer = affiliated(TODAY_IN_LIMA.plusWeeks(1), OfferStatus.PUBLISHED);
        when(offerRepository.findById(OFFER_ID)).thenReturn(Optional.of(offer));
        when(merchantStandingRepository.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.of(standing()));

        var detail = service.getDetail(new GetOfferByIdQuery(OFFER_ID));

        assertThat(detail.offer().getLocation()).isEqualTo(STORE);
        assertThat(detail.businessName()).isEqualTo(BUSINESS_NAME);
        assertThat(detail.verifiedSeal()).isTrue();
        assertThat(detail.available()).isTrue();
    }

    @Test
    void unknownOfferIsNotFound() {
        when(offerRepository.findById(OFFER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(new GetOfferByIdQuery(OFFER_ID)))
                .isInstanceOf(OfferNotFoundException.class)
                .hasMessage("La oferta 1052 no existe.");
    }

    @Test
    void publishedOfferPastItsValidityIsNotAvailable() {
        when(offerRepository.findById(OFFER_ID))
                .thenReturn(Optional.of(affiliated(TODAY_IN_LIMA.minusDays(1), OfferStatus.PUBLISHED)));

        assertThat(service.getDetail(new GetOfferByIdQuery(OFFER_ID)).available()).isFalse();
    }

    @Test
    void expiredOfferIsShownAsNotAvailable() {
        when(offerRepository.findById(OFFER_ID))
                .thenReturn(Optional.of(affiliated(TODAY_IN_LIMA.plusWeeks(1), OfferStatus.EXPIRED)));

        assertThat(service.getDetail(new GetOfferByIdQuery(OFFER_ID)).available()).isFalse();
    }

    @Test
    void removedOfferIsNotFound() {
        when(offerRepository.findById(OFFER_ID))
                .thenReturn(Optional.of(affiliated(TODAY_IN_LIMA.plusWeeks(1), OfferStatus.REMOVED)));

        assertThatThrownBy(() -> service.getDetail(new GetOfferByIdQuery(OFFER_ID)))
                .isInstanceOf(OfferNotFoundException.class);
    }

    @Test
    void offerIsStillAvailableOnItsLastDayInLimaWhenUtcIsAlreadyTheNextDay() {
        var lateNightInLima = new OfferQueryService(offerRepository, merchantStandingRepository,
                Clock.fixed(LATE_NIGHT_IN_LIMA, ZoneOffset.UTC));
        when(offerRepository.findById(OFFER_ID))
                .thenReturn(Optional.of(affiliated(TODAY_IN_LIMA, OfferStatus.PUBLISHED)));

        assertThat(lateNightInLima.getDetail(new GetOfferByIdQuery(OFFER_ID)).available()).isTrue();
    }

    @Test
    void publicSourceOfferShowsItsSourceWithoutSeal() {
        when(offerRepository.findById(OFFER_ID)).thenReturn(Optional.of(publicSource()));

        var detail = service.getDetail(new GetOfferByIdQuery(OFFER_ID));

        assertThat(detail.offer().getBusinessId()).isNull();
        assertThat(detail.businessName()).isEqualTo(SOURCE_NAME);
        assertThat(detail.verifiedSeal()).isFalse();
        verifyNoInteractions(merchantStandingRepository);
    }

    @Test
    void offerWithoutMerchantCopyHasNoNameNorSeal() {
        when(offerRepository.findById(OFFER_ID))
                .thenReturn(Optional.of(affiliated(TODAY_IN_LIMA, OfferStatus.PUBLISHED)));
        when(merchantStandingRepository.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.empty());

        var detail = service.getDetail(new GetOfferByIdQuery(OFFER_ID));

        assertThat(detail.businessName()).isNull();
        assertThat(detail.verifiedSeal()).isFalse();
    }

    @Test
    void validOfferCanBeReservedWithTheFieldsReservationReads() {
        when(offerRepository.findById(OFFER_ID))
                .thenReturn(Optional.of(affiliated(TODAY_IN_LIMA.plusWeeks(1), OfferStatus.PUBLISHED)));

        var availability = service.getAvailability(new GetOfferByIdQuery(OFFER_ID));

        assertThat(availability.offerId()).isEqualTo(OFFER_ID);
        assertThat(availability.businessId()).isEqualTo(BUSINESS_ID);
        assertThat(availability.title()).isEqualTo("2x1 en almuerzos ejecutivos");
        assertThat(availability.validTo()).isEqualTo(TODAY_IN_LIMA.plusWeeks(1));
        assertThat(availability.available()).isTrue();
    }

    @Test
    void expiredOfferCannotBeReserved() {
        when(offerRepository.findById(OFFER_ID))
                .thenReturn(Optional.of(affiliated(TODAY_IN_LIMA.minusDays(1), OfferStatus.PUBLISHED)));

        assertThat(service.getAvailability(new GetOfferByIdQuery(OFFER_ID)).available()).isFalse();
    }

    @Test
    void removedOfferCannotBeReserved() {
        when(offerRepository.findById(OFFER_ID))
                .thenReturn(Optional.of(affiliated(TODAY_IN_LIMA.plusWeeks(1), OfferStatus.REMOVED)));

        assertThat(service.getAvailability(new GetOfferByIdQuery(OFFER_ID)).available()).isFalse();
    }

    @Test
    void publicSourceOfferCannotBeReserved() {
        when(offerRepository.findById(OFFER_ID)).thenReturn(Optional.of(publicSource()));

        var availability = service.getAvailability(new GetOfferByIdQuery(OFFER_ID));

        assertThat(availability.available()).isFalse();
        assertThat(availability.businessId()).isNull();
    }

    @Test
    void availabilityOfAnUnknownOfferIsNotFound() {
        when(offerRepository.findById(OFFER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAvailability(new GetOfferByIdQuery(OFFER_ID)))
                .isInstanceOf(OfferNotFoundException.class);
    }

    private static Offer publicSource() {
        return new Offer(OFFER_ID, null, null, "Menú del día a S/ 10", "Solo en el local", Money.soles(PRICE),
                TODAY_IN_LIMA, "Gastronomía", GeocodingStatus.GEOCODED, "Av. Grau 120, La Victoria", null,
                OfferSource.PUBLIC_SOURCE, SOURCE_NAME, OfferStatus.PUBLISHED, STORE);
    }

    static Offer affiliated(LocalDate validTo, OfferStatus status) {
        return new Offer(OFFER_ID, CAMPAIGN_ID, BUSINESS_ID, "2x1 en almuerzos ejecutivos",
                "Válido de lunes a viernes de 12:00 a 15:00. Un cupón por mesa.", Money.soles(PRICE), validTo,
                "Gastronomía", GeocodingStatus.GEOCODED, "Av. Larco 345, Miraflores", null, OfferSource.AFFILIATED,
                null, status, STORE);
    }

    private static MerchantStanding standing() {
        return new MerchantStanding(BUSINESS_ID, BUSINESS_NAME, true, 0, FULL_COMPLIANCE, true,
                LocalDateTime.parse("2026-10-01T12:00:00"));
    }
}
