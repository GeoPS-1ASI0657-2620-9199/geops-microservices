package com.geopslabs.geops.reservation.domain.models;

import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationAccessDeniedException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationAlreadyRedeemedException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationExpiredException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationReportedException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T17:42:10Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC);
    private static final LocalDateTime RESERVED_AT = LocalDateTime.parse("2026-10-14T15:00:00");
    private static final LocalDateTime EXPIRES_AT = LocalDateTime.parse("2026-10-21T04:59:59");
    private static final LocalDateTime FIRST_REDEMPTION = LocalDateTime.parse("2026-10-20T16:00:00");
    private static final Long BUSINESS_ID = 84L;
    private static final Long OTHER_BUSINESS_ID = 99L;

    @Test
    void redeemsAnActiveReservationOfTheBusinessWithinItsDeadline() {
        var redeemed = reservation(ReservationStatus.ACTIVE, EXPIRES_AT, null).redeem(BUSINESS_ID, NOW);

        assertThat(redeemed.getStatus()).isEqualTo(ReservationStatus.REDEEMED);
        assertThat(redeemed.getRedeemedAt()).isEqualTo(NOW);
        assertThat(redeemed.getId()).isEqualTo(5012L);
    }

    @Test
    void rejectsTheCodeOfAnotherBusiness() {
        var reservation = reservation(ReservationStatus.ACTIVE, EXPIRES_AT, null);

        assertThat(reservation.rejectionFor(OTHER_BUSINESS_ID, NOW)).contains(RedeemRejection.OTHER_BUSINESS);
        assertThatThrownBy(() -> reservation.redeem(OTHER_BUSINESS_ID, NOW))
                .isInstanceOf(ReservationAccessDeniedException.class);
    }

    @Test
    void rejectsASecondRedemptionWithTheDateOfTheFirst() {
        var reservation = reservation(ReservationStatus.REDEEMED, EXPIRES_AT, FIRST_REDEMPTION);

        assertThat(reservation.rejectionFor(BUSINESS_ID, NOW)).contains(RedeemRejection.ALREADY_REDEEMED);
        assertThatThrownBy(() -> reservation.redeem(BUSINESS_ID, NOW))
                .isInstanceOfSatisfying(ReservationAlreadyRedeemedException.class,
                        exception -> assertThat(exception.getRedeemedAt()).isEqualTo(FIRST_REDEMPTION));
    }

    @Test
    void rejectsAReportedReservation() {
        var reservation = reservation(ReservationStatus.REPORTED, EXPIRES_AT, null);

        assertThat(reservation.rejectionFor(BUSINESS_ID, NOW)).contains(RedeemRejection.REPORTED);
        assertThatThrownBy(() -> reservation.redeem(BUSINESS_ID, NOW))
                .isInstanceOf(ReservationReportedException.class);
    }

    @Test
    void rejectsAnActiveReservationPastItsDeadline() {
        var reservation = reservation(ReservationStatus.ACTIVE, NOW.minusSeconds(1), null);

        assertThat(reservation.rejectionFor(BUSINESS_ID, NOW)).contains(RedeemRejection.EXPIRED);
        assertThatThrownBy(() -> reservation.redeem(BUSINESS_ID, NOW))
                .isInstanceOf(ReservationExpiredException.class);
    }

    @Test
    void treatsTheDeadlineAsExclusive() {
        var reservation = reservation(ReservationStatus.ACTIVE, NOW, null);

        assertThat(reservation.rejectionFor(BUSINESS_ID, NOW)).contains(RedeemRejection.EXPIRED);
    }

    @Test
    void rejectsAReservationAlreadyMarkedExpired() {
        var reservation = reservation(ReservationStatus.EXPIRED, EXPIRES_AT, null);

        assertThat(reservation.rejectionFor(BUSINESS_ID, NOW)).contains(RedeemRejection.EXPIRED);
    }

    @Test
    void explainsAnotherBusinessBeforeTheStateOfTheReservation() {
        var reservation = reservation(ReservationStatus.REDEEMED, EXPIRES_AT, FIRST_REDEMPTION);

        assertThat(reservation.rejectionFor(OTHER_BUSINESS_ID, NOW)).contains(RedeemRejection.OTHER_BUSINESS);
    }

    private static Reservation reservation(ReservationStatus status, LocalDateTime expiresAt,
                                           LocalDateTime redeemedAt) {
        return new Reservation(5012L, new ReservationCode("7KQ2M9XA"), 2001L, 1052L, BUSINESS_ID,
                "2x1 en almuerzos ejecutivos", RESERVED_AT, expiresAt, redeemedAt, status);
    }
}
