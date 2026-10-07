package com.geopslabs.geops.reservation.application.services;

import com.geopslabs.geops.reservation.domain.models.OfferSnapshot;
import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.ReservationCode;
import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import com.geopslabs.geops.reservation.domain.models.commands.CreateReservationCommand;
import com.geopslabs.geops.reservation.domain.models.exceptions.ActiveReservationAlreadyExistsException;
import com.geopslabs.geops.reservation.domain.models.exceptions.CatalogUnavailableException;
import com.geopslabs.geops.reservation.domain.models.exceptions.OfferNotAvailableException;
import com.geopslabs.geops.reservation.domain.models.exceptions.OfferNotFoundException;
import com.geopslabs.geops.reservation.domain.ports.OfferCatalogPort;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationCommandServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-08T13:05:00Z");
    private static final long SEED = 208L;
    private static final Long CONSUMER_ID = 2001L;
    private static final Long OFFER_ID = 1052L;
    private static final Long BUSINESS_ID = 301L;
    private static final Long RESERVATION_ID = 15L;
    private static final String TITLE = "Menú ejecutivo a mitad de precio";
    private static final LocalDate VALID_TO = LocalDate.parse("2026-10-14");
    private static final LocalDateTime END_OF_VALID_TO_IN_LIMA = LocalDateTime.parse("2026-10-15T04:59:59");
    private static final LocalDate LIMA_TODAY = LocalDate.parse("2026-10-08");
    private static final LocalDate LIMA_YESTERDAY = LocalDate.parse("2026-10-07");
    private static final Instant LIMA_LATE_NIGHT = Instant.parse("2026-10-09T03:30:00Z");
    private static final ReservationCode EXISTING_CODE = new ReservationCode("K7P3XM9Q");
    private static final CreateReservationCommand COMMAND = new CreateReservationCommand(CONSUMER_ID, OFFER_ID);

    @Mock
    private ReservationRepositoryPort reservationRepository;
    @Mock
    private OfferCatalogPort offerCatalog;

    private ReservationCommandService service;

    @BeforeEach
    void setUp() {
        service = serviceAt(NOW);
    }

    @Test
    void createsActiveReservationWithTitleAndBusinessOfTheOffer() {
        givenOfferValidTo(VALID_TO);
        when(reservationRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        var result = service.create(COMMAND);

        var reservation = result.reservation();
        assertThat(result.created()).isTrue();
        assertThat(reservation.getId()).isEqualTo(RESERVATION_ID);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reservation.getConsumerId()).isEqualTo(CONSUMER_ID);
        assertThat(reservation.getBusinessId()).isEqualTo(BUSINESS_ID);
        assertThat(reservation.getOfferTitle()).isEqualTo(TITLE);
        assertThat(reservation.getReservedAt()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(reservation.getRedeemedAt()).isNull();
        assertThat(ReservationCode.isWellFormed(reservation.getCode().value())).isTrue();
    }

    @Test
    void expiresAtTheEndOfTheLastValidDayInLima() {
        givenOfferValidTo(VALID_TO);
        when(reservationRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        var result = service.create(COMMAND);

        assertThat(result.reservation().getExpiresAt()).isEqualTo(END_OF_VALID_TO_IN_LIMA);
    }

    @Test
    void acceptsAnOfferThatEndsTodayInLima() {
        givenOfferValidTo(LIMA_TODAY);
        when(reservationRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        assertThat(service.create(COMMAND).created()).isTrue();
    }

    @Test
    void rejectsAnOfferThatEndedYesterdayInLimaWithoutSaving() {
        givenOfferValidTo(LIMA_YESTERDAY);

        assertThatThrownBy(() -> service.create(COMMAND)).isInstanceOf(OfferNotAvailableException.class);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void usesTheLimaDateEvenWhenUtcIsAlreadyTheNextDay() {
        service = serviceAt(LIMA_LATE_NIGHT);
        givenOfferValidTo(LIMA_TODAY);
        when(reservationRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        assertThat(service.create(COMMAND).created()).isTrue();
    }

    @Test
    void propagatesOfferNotFound() {
        when(offerCatalog.findValidOffer(OFFER_ID)).thenThrow(new OfferNotFoundException(OFFER_ID));

        assertThatThrownBy(() -> service.create(COMMAND)).isInstanceOf(OfferNotFoundException.class);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void propagatesCatalogUnavailable() {
        when(offerCatalog.findValidOffer(OFFER_ID)).thenThrow(new CatalogUnavailableException());

        assertThatThrownBy(() -> service.create(COMMAND)).isInstanceOf(CatalogUnavailableException.class);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void returnsTheActiveReservationWithoutGeneratingACode() {
        givenOfferValidTo(VALID_TO);
        var existing = storedReservation();
        when(reservationRepository.findActiveByConsumerAndOffer(CONSUMER_ID, OFFER_ID))
                .thenReturn(Optional.of(existing));

        var result = service.create(COMMAND);

        assertThat(result.created()).isFalse();
        assertThat(result.reservation()).isSameAs(existing);
        verify(reservationRepository, never()).existsByCode(anyString());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void retriesTheCodeWhileItIsTaken() {
        givenOfferValidTo(VALID_TO);
        when(reservationRepository.existsByCode(anyString())).thenReturn(true, true, false);
        when(reservationRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        service.create(COMMAND);

        var codes = ArgumentCaptor.forClass(String.class);
        verify(reservationRepository, times(3)).existsByCode(codes.capture());
        var saved = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(saved.capture());
        assertThat(saved.getValue().getCode().value()).isEqualTo(codes.getAllValues().get(2));
    }

    @Test
    void givesUpAfterTheMaximumNumberOfCodeAttempts() {
        givenOfferValidTo(VALID_TO);
        when(reservationRepository.existsByCode(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.create(COMMAND)).isInstanceOf(IllegalStateException.class);
        verify(reservationRepository, times(ReservationCommandService.MAX_CODE_ATTEMPTS)).existsByCode(anyString());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void returnsTheReservationOfAConcurrentRequestWhenTheIndexRejectsTheInsert() {
        givenOfferValidTo(VALID_TO);
        var concurrent = storedReservation();
        when(reservationRepository.findActiveByConsumerAndOffer(CONSUMER_ID, OFFER_ID))
                .thenReturn(Optional.empty(), Optional.of(concurrent));
        when(reservationRepository.save(any()))
                .thenThrow(new ActiveReservationAlreadyExistsException(CONSUMER_ID, OFFER_ID));

        var result = service.create(COMMAND);

        assertThat(result.created()).isFalse();
        assertThat(result.reservation()).isSameAs(concurrent);
    }

    private ReservationCommandService serviceAt(Instant instant) {
        return new ReservationCommandService(reservationRepository, offerCatalog, Clock.fixed(instant, ZoneOffset.UTC),
                new Random(SEED));
    }

    private void givenOfferValidTo(LocalDate validTo) {
        when(offerCatalog.findValidOffer(OFFER_ID)).thenReturn(new OfferSnapshot(OFFER_ID, BUSINESS_ID, TITLE, validTo));
    }

    private static Reservation withId(Reservation reservation) {
        return new Reservation(RESERVATION_ID, reservation.getCode(), reservation.getConsumerId(),
                reservation.getOfferId(), reservation.getBusinessId(), reservation.getOfferTitle(),
                reservation.getReservedAt(), reservation.getExpiresAt(), reservation.getRedeemedAt(),
                reservation.getStatus());
    }

    private static Reservation storedReservation() {
        return new Reservation(RESERVATION_ID, EXISTING_CODE, CONSUMER_ID, OFFER_ID, BUSINESS_ID, TITLE,
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC), END_OF_VALID_TO_IN_LIMA, null, ReservationStatus.ACTIVE);
    }
}
