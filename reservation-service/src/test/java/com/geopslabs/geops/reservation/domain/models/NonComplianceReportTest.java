package com.geopslabs.geops.reservation.domain.models;

import com.geopslabs.geops.reservation.domain.models.exceptions.InvalidReportAnswerException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReportAlreadyClosedException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReportNotOpenException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReportResponseDeadlinePassedException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NonComplianceReportTest {
    private static final LocalDateTime OPENED_AT = LocalDateTime.parse("2026-10-28T19:10:00");
    private static final LocalDateTime DEADLINE = LocalDateTime.parse("2026-10-30T19:10:00");
    private static final String ANSWER = "El 2x1 aplicaba solo de lunes a jueves.";

    @Test
    void opensWithTheDeadlineOfTheWindowAndTheDataOfTheReservation() {
        var reported = new Reservation(5013L, new ReservationCode("Q4ZT8K2M"), 2001L, 1052L, 84L,
                "2x1 en almuerzos ejecutivos", OPENED_AT.minusDays(1), OPENED_AT.plusDays(1), null,
                ReservationStatus.REPORTED);

        var report = NonComplianceReport.open(reported, new ReportReason("No me aplicaron el 2x1."), OPENED_AT,
                Duration.ofHours(48));

        assertThat(report.getStatus()).isEqualTo(ReportStatus.OPEN);
        assertThat(report.getOpenedAt()).isEqualTo(OPENED_AT);
        assertThat(report.getResponseDeadline()).isEqualTo(DEADLINE);
        assertThat(report.getReservationId()).isEqualTo(5013L);
        assertThat(report.getConsumerId()).isEqualTo(2001L);
        assertThat(report.getBusinessId()).isEqualTo(84L);
        assertThat(report.getResolution()).isNull();
    }

    @Test
    void anAnswerBeforeTheDeadlineMovesTheReportToReview() {
        var answered = open().answer(ANSWER, DEADLINE.minusSeconds(1));

        assertThat(answered.getStatus()).isEqualTo(ReportStatus.IN_REVIEW);
        assertThat(answered.getAnswer().text()).isEqualTo(ANSWER);
        assertThat(answered.getAnswer().respondedAt()).isEqualTo(DEADLINE.minusSeconds(1));
    }

    @Test
    void anAnswerAtOrAfterTheDeadlineIsRejectedEvenIfTheSchedulerHasNotClosedTheReport() {
        assertThatThrownBy(() -> open().answer(ANSWER, DEADLINE))
                .isInstanceOf(ReportResponseDeadlinePassedException.class);
        assertThatThrownBy(() -> open().answer(ANSWER, DEADLINE.plusMinutes(1)))
                .isInstanceOf(ReportResponseDeadlinePassedException.class);
    }

    @Test
    void aReportThatIsNotOpenTakesNoAnswer() {
        var inReview = open().answer(ANSWER, OPENED_AT.plusHours(1));
        var closed = open().resolve(ReportResolution.CONSUMER_FAVOR, OPENED_AT.plusHours(3));

        assertThatThrownBy(() -> inReview.answer(ANSWER, OPENED_AT.plusHours(2)))
                .isInstanceOf(ReportNotOpenException.class);
        assertThatThrownBy(() -> closed.answer(ANSWER, OPENED_AT.plusHours(4)))
                .isInstanceOf(ReportNotOpenException.class);
    }

    @Test
    void anAnswerOutsideTenToTwoThousandCharactersIsRejected() {
        assertThatThrownBy(() -> open().answer("corto", OPENED_AT.plusHours(1)))
                .isInstanceOf(InvalidReportAnswerException.class);
        assertThatThrownBy(() -> open().answer("x".repeat(2001), OPENED_AT.plusHours(1)))
                .isInstanceOf(InvalidReportAnswerException.class);
    }

    @Test
    void resolvingClosesFromOpenOrInReviewAndKeepsTheAnswer() {
        var now = OPENED_AT.plusHours(5);

        var fromOpen = open().resolve(ReportResolution.CONSUMER_FAVOR, now);
        var fromReview = open().answer(ANSWER, OPENED_AT.plusHours(1)).resolve(ReportResolution.CONSUMER_FAVOR, now);

        assertThat(fromOpen.getStatus()).isEqualTo(ReportStatus.CLOSED);
        assertThat(fromOpen.getResolution()).isEqualTo(ReportResolution.CONSUMER_FAVOR);
        assertThat(fromOpen.getResolvedAt()).isEqualTo(now);
        assertThat(fromReview.getAnswer().text()).isEqualTo(ANSWER);
    }

    @Test
    void aClosedReportCannotBeResolvedAgain() {
        var closed = open().resolve(ReportResolution.CONSUMER_FAVOR, OPENED_AT.plusHours(5));

        assertThatThrownBy(() -> closed.resolve(ReportResolution.CONSUMER_FAVOR, OPENED_AT.plusHours(6)))
                .isInstanceOf(ReportAlreadyClosedException.class);
    }

    @Test
    void theDeadlineClosesAnOpenReportInFavorOfTheConsumer() {
        var closed = open().closeByDeadline(DEADLINE.plusMinutes(5));

        assertThat(closed.getStatus()).isEqualTo(ReportStatus.CLOSED);
        assertThat(closed.getResolution()).isEqualTo(ReportResolution.CONSUMER_FAVOR);
        assertThat(closed.getResolvedAt()).isEqualTo(DEADLINE.plusMinutes(5));
    }

    @Test
    void theDeadlineDoesNotTouchAReportThatWasAnsweredOrStillHasTime() {
        var answered = open().answer(ANSWER, OPENED_AT.plusHours(1));

        assertThatThrownBy(() -> answered.closeByDeadline(DEADLINE.plusMinutes(5)))
                .isInstanceOf(ReportNotOpenException.class);
        assertThatThrownBy(() -> open().closeByDeadline(DEADLINE.minusSeconds(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static NonComplianceReport open() {
        return new NonComplianceReport(31L, 5013L, 2001L, 84L, new ReportReason("No me aplicaron el 2x1."),
                OPENED_AT, DEADLINE, ReportStatus.OPEN, null, null, null);
    }
}
