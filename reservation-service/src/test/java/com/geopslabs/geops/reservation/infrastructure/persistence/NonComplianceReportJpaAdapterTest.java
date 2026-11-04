package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.models.NonComplianceReport;
import com.geopslabs.geops.reservation.domain.models.ReportReason;
import com.geopslabs.geops.reservation.domain.models.ReportResolution;
import com.geopslabs.geops.reservation.domain.models.ReportStatus;
import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.ReservationCode;
import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReportAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The reports and their answers against a real PostgreSQL, with the indexes of V5 applied by Flyway. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({NonComplianceReportJpaAdapter.class, ReservationJpaAdapter.class})
class NonComplianceReportJpaAdapterTest {
    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres");
    private static final LocalDateTime NOW = LocalDateTime.parse("2026-10-28T19:10:00");

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE);

    static {
        POSTGRES.start();
    }

    @Autowired
    private NonComplianceReportJpaAdapter reports;
    @Autowired
    private ReservationJpaAdapter reservations;

    private int sequence;

    @BeforeEach
    void emptyTables() {
        sequence = 0;
    }

    @Test
    void savesAnOpenReportAndReadsItBackWithItsDeadline() {
        var saved = reports.save(openReport(reservation(ReservationStatus.REPORTED, NOW.plusDays(1))));

        var read = reports.findById(saved.getId()).orElseThrow();

        assertThat(read.getStatus()).isEqualTo(ReportStatus.OPEN);
        assertThat(read.getReason().value()).isEqualTo("No me aplicaron el 2x1.");
        assertThat(read.getOpenedAt()).isEqualTo(NOW);
        assertThat(read.getResponseDeadline()).isEqualTo(NOW.plusHours(48));
        assertThat(read.getAnswer()).isNull();
    }

    @Test
    void aSecondReportForTheSameReservationIsRejectedByTheUniqueIndex() {
        var reservation = reservation(ReservationStatus.REPORTED, NOW.plusDays(1));
        reports.save(openReport(reservation));

        assertThatThrownBy(() -> reports.save(openReport(reservation)))
                .isInstanceOf(ReportAlreadyExistsException.class);
    }

    @Test
    void anAnswerIsStoredOnceAndComesBackWithTheReport() {
        var saved = reports.save(openReport(reservation(ReservationStatus.REPORTED, NOW.plusDays(1))));

        var answered = reports.save(saved.answer("El 2x1 aplicaba de lunes a jueves.", NOW.plusHours(2)));
        var read = reports.findById(answered.getId()).orElseThrow();

        assertThat(read.getStatus()).isEqualTo(ReportStatus.IN_REVIEW);
        assertThat(read.getAnswer().text()).isEqualTo("El 2x1 aplicaba de lunes a jueves.");
        assertThat(read.getAnswer().id()).isNotNull();
        assertThat(read.getAnswer().respondedAt()).isEqualTo(NOW.plusHours(2));
    }

    @Test
    void closingKeepsTheAnswerAndTheResolution() {
        var saved = reports.save(openReport(reservation(ReservationStatus.REPORTED, NOW.plusDays(1))));
        var answered = reports.save(saved.answer("El 2x1 aplicaba de lunes a jueves.", NOW.plusHours(2)));

        var closed = reports.save(reports.findByIdForUpdate(answered.getId()).orElseThrow()
                .resolve(ReportResolution.CONSUMER_FAVOR, NOW.plusHours(3)));

        var read = reports.findById(closed.getId()).orElseThrow();
        assertThat(read.getStatus()).isEqualTo(ReportStatus.CLOSED);
        assertThat(read.getResolution()).isEqualTo(ReportResolution.CONSUMER_FAVOR);
        assertThat(read.getResolvedAt()).isEqualTo(NOW.plusHours(3));
        assertThat(read.getAnswer().id()).isEqualTo(answered.getAnswer().id());
    }

    @Test
    void listsByBusinessAndByConsumerNewestFirstFilteredByStatus() {
        var first = reports.save(openReport(reservation(ReservationStatus.REPORTED, NOW.plusDays(1)), NOW));
        var second = reports.save(openReport(reservation(ReservationStatus.REPORTED, NOW.plusDays(1)), NOW.plusHours(1)));
        reports.save(second.answer("El 2x1 aplicaba de lunes a jueves.", NOW.plusHours(2)));

        assertThat(ids(reports.findByBusiness(84L, null))).containsExactly(second.getId(), first.getId());
        assertThat(ids(reports.findByBusiness(84L, ReportStatus.OPEN))).containsExactly(first.getId());
        assertThat(ids(reports.findByBusiness(84L, ReportStatus.IN_REVIEW))).containsExactly(second.getId());
        assertThat(ids(reports.findByConsumer(2001L, ReportStatus.CLOSED))).isEmpty();
        assertThat(reports.findByBusiness(99L, null)).isEmpty();
        assertThat(reports.findByBusiness(84L, ReportStatus.IN_REVIEW).get(0).getAnswer()).isNotNull();
    }

    @Test
    void findsTheReportsOfAPageOfReservationsInOneCall() {
        var one = reservation(ReservationStatus.REPORTED, NOW.plusDays(1));
        var other = reservation(ReservationStatus.ACTIVE, NOW.plusDays(1));
        var report = reports.save(openReport(one));

        var found = reports.findByReservationIds(List.of(one.getId(), other.getId()));

        assertThat(ids(found)).containsExactly(report.getId());
        assertThat(reports.findByReservationIds(List.of())).isEmpty();
    }

    @Test
    void theSchedulerSeesOnlyOpenReportsPastTheirDeadlineOldestFirstAndWithALimit() {
        var late = reports.save(openReport(reservation(ReservationStatus.REPORTED, NOW.plusDays(1)), NOW.minusDays(5)));
        var later = reports.save(openReport(reservation(ReservationStatus.REPORTED, NOW.plusDays(1)), NOW.minusDays(4)));
        var inTime = reports.save(openReport(reservation(ReservationStatus.REPORTED, NOW.plusDays(1)), NOW));
        var answered = reports.save(openReport(reservation(ReservationStatus.REPORTED, NOW.plusDays(1)), NOW.minusDays(6)));
        reports.save(answered.answer("Respuesta a tiempo del comercio.", answered.getOpenedAt().plusHours(1)));

        assertThat(reports.findOpenIdsPastDeadline(NOW, 10)).containsExactly(late.getId(), later.getId());
        assertThat(reports.findOpenIdsPastDeadline(NOW, 1)).containsExactly(late.getId());
        assertThat(reports.findOpenIdsPastDeadline(inTime.getResponseDeadline(), 10)).contains(inTime.getId());
    }

    @Test
    void expiringMarksOnlyTheActiveReservationsPastTheirDeadlineAndRunsTwiceWithoutChange() {
        var past = reservation(ReservationStatus.ACTIVE, NOW.minusMinutes(1));
        var exactly = reservation(ReservationStatus.ACTIVE, NOW);
        var future = reservation(ReservationStatus.ACTIVE, NOW.plusMinutes(1));
        var redeemed = reservation(ReservationStatus.REDEEMED, NOW.minusDays(1));

        assertThat(reservations.expireActiveUntil(NOW)).isEqualTo(2);
        assertThat(reservations.expireActiveUntil(NOW)).isZero();

        assertThat(status(past)).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(status(exactly)).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(status(future)).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(status(redeemed)).isEqualTo(ReservationStatus.REDEEMED);
    }

    private ReservationStatus status(Reservation reservation) {
        return reservations.findById(reservation.getId()).orElseThrow().getStatus();
    }

    private static List<Long> ids(List<NonComplianceReport> found) {
        return found.stream().map(NonComplianceReport::getId).toList();
    }

    private static NonComplianceReport openReport(Reservation reported) {
        return openReport(reported, NOW);
    }

    private static NonComplianceReport openReport(Reservation reported, LocalDateTime openedAt) {
        return NonComplianceReport.open(reported, new ReportReason("No me aplicaron el 2x1."), openedAt,
                Duration.ofHours(48));
    }

    private Reservation reservation(ReservationStatus status, LocalDateTime expiresAt) {
        var number = ++sequence;
        return reservations.save(new Reservation(null, new ReservationCode("Q4ZTAAA" + ReservationCode.ALPHABET.charAt(number)), 2001L, 1000L + number,
                84L, "2x1 en almuerzos ejecutivos", NOW.minusDays(2), expiresAt, null, status));
    }
}
