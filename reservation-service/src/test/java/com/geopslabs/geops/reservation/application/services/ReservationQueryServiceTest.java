package com.geopslabs.geops.reservation.application.services;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.ReservationCode;
import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationAccessDeniedException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationNotFoundException;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByCodeQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByIdQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationsByConsumerIdQuery;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationQueryServiceTest {
    private static final Long RESERVATION_ID = 15L;
    private static final Long CONSUMER_ID = 2001L;
    private static final Long OTHER_CONSUMER_ID = 2002L;
    private static final Long BUSINESS_ID = 301L;
    private static final Long OTHER_BUSINESS_ID = 302L;
    private static final Long OFFER_ID = 1052L;
    private static final String CODE = "K7P3XM9Q";
    private static final String UNKNOWN_CODE = "ZZZZ2222";
    private static final LocalDateTime RESERVED_AT = LocalDateTime.parse("2026-10-08T13:05:00");
    private static final LocalDateTime EXPIRES_AT = LocalDateTime.parse("2026-10-15T04:59:59");

    @Mock
    private ReservationRepositoryPort reservationRepository;

    private ReservationQueryService service;

    @BeforeEach
    void setUp() {
        service = new ReservationQueryService(reservationRepository);
    }

    @Test
    void returnsTheReservationOfTheConsumer() {
        var reservation = reservation();
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

        assertThat(service.getById(new GetReservationByIdQuery(RESERVATION_ID, CONSUMER_ID))).isSameAs(reservation);
    }

    @Test
    void unknownIdIsNotFound() {
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(new GetReservationByIdQuery(RESERVATION_ID, CONSUMER_ID)))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void reservationOfAnotherConsumerIsForbidden() {
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation()));

        assertThatThrownBy(() -> service.getById(new GetReservationByIdQuery(RESERVATION_ID, OTHER_CONSUMER_ID)))
                .isInstanceOf(ReservationAccessDeniedException.class);
    }

    @Test
    void returnsTheReservationOfTheCodeToItsBusiness() {
        var reservation = reservation();
        when(reservationRepository.findByCode(CODE)).thenReturn(Optional.of(reservation));

        assertThat(service.getByCode(new GetReservationByCodeQuery(CODE, BUSINESS_ID))).isSameAs(reservation);
    }

    @Test
    void unknownCodeIsNotFound() {
        when(reservationRepository.findByCode(UNKNOWN_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByCode(new GetReservationByCodeQuery(UNKNOWN_CODE, BUSINESS_ID)))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void codeOfAnotherBusinessIsForbidden() {
        when(reservationRepository.findByCode(CODE)).thenReturn(Optional.of(reservation()));

        assertThatThrownBy(() -> service.getByCode(new GetReservationByCodeQuery(CODE, OTHER_BUSINESS_ID)))
                .isInstanceOf(ReservationAccessDeniedException.class);
    }

    @Test
    void listsTheReservationsOfTheConsumerFilteredByStatus() {
        var reservations = List.of(reservation());
        when(reservationRepository.findByConsumerId(CONSUMER_ID, ReservationStatus.ACTIVE)).thenReturn(reservations);

        var result = service.list(new GetReservationsByConsumerIdQuery(CONSUMER_ID, ReservationStatus.ACTIVE));

        assertThat(result).isEqualTo(reservations);
        verify(reservationRepository).findByConsumerId(CONSUMER_ID, ReservationStatus.ACTIVE);
    }

    @Test
    void listsEveryReservationOfTheConsumerWithoutStatus() {
        when(reservationRepository.findByConsumerId(CONSUMER_ID, null)).thenReturn(List.of());

        assertThat(service.list(new GetReservationsByConsumerIdQuery(CONSUMER_ID, null))).isEmpty();
    }

    private static Reservation reservation() {
        return new Reservation(RESERVATION_ID, new ReservationCode(CODE), CONSUMER_ID, OFFER_ID, BUSINESS_ID,
                "Menú ejecutivo a mitad de precio", RESERVED_AT, EXPIRES_AT, null, ReservationStatus.ACTIVE);
    }
}
