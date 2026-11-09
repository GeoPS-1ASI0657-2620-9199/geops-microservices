package com.geopslabs.geops.reservation.infrastructure.messaging;

import com.geopslabs.geops.reservation.domain.events.DomainEvent;
import com.geopslabs.geops.reservation.domain.events.ReportAnswered;
import com.geopslabs.geops.reservation.domain.events.ReportOpened;
import com.geopslabs.geops.reservation.domain.events.ReportResolved;
import com.geopslabs.geops.reservation.domain.models.NonComplianceReport;
import com.geopslabs.geops.reservation.domain.models.ReportClosedBy;
import com.geopslabs.geops.reservation.domain.models.ReportReason;
import com.geopslabs.geops.reservation.domain.models.ReportResolution;
import com.geopslabs.geops.reservation.domain.models.ReportStatus;
import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.ReservationCode;
import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxEventJpaEntity;
import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxEventJpaRepository;
import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxJpaAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** The three report events have the shape of the examples of BRD-11 that Catalog and Notification build on. */
class ReportEventsContractTest {
    private static final LocalDateTime OPENED_AT = LocalDateTime.parse("2026-10-28T19:10:00");
    private static final UUID EVENT_ID = UUID.fromString("9d8c7b6a-5f4e-4d3c-8b2a-1f0e9d8c7b6a");

    private final OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
    private final OutboxJpaAdapter adapter = new OutboxJpaAdapter(repository, Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build(),
            Clock.fixed(Instant.parse("2026-10-28T19:10:00Z"), ZoneOffset.UTC));

    private final Reservation reservation = new Reservation(5013L, new ReservationCode("Q4ZT8K2M"), 2001L, 1052L, 84L,
            "2x1 en almuerzos ejecutivos", OPENED_AT.minusDays(1), OPENED_AT.plusDays(1), null,
            ReservationStatus.REPORTED);
    private final NonComplianceReport open = new NonComplianceReport(31L, 5013L, 2001L, 84L,
            new ReportReason("No me aplicaron el 2x1 y me cobraron los dos platos."), OPENED_AT,
            OPENED_AT.plus(Duration.ofHours(48)), ReportStatus.OPEN, null, null, null);

    @Test
    void reportOpenedHasTheShapeOfTheExample() {
        adapter.append(ReportOpened.of(open, reservation, EVENT_ID));

        EventContract.assertSameShape(written().getPayload(), "ReportOpened");
        assertThat(written().getAggregateId()).isEqualTo(5013L);
        assertThat(written().getPayload()).contains("\"occurredAt\":\"2026-10-28T19:10:00Z\"",
                "\"responseDeadline\":\"2026-10-30T19:10:00Z\"", "\"offerId\":1052");
    }

    @Test
    void reportAnsweredHasTheShapeOfTheExample() {
        var answered = open.answer("El 2x1 aplicaba solo de lunes a jueves.", OPENED_AT.plusHours(17));

        adapter.append(ReportAnswered.of(answered, reservation, EVENT_ID));

        EventContract.assertSameShape(written().getPayload(), "ReportAnswered");
        assertThat(written().getPayload()).contains("\"answeredAt\":\"2026-10-29T12:10:00Z\"");
    }

    @Test
    void reportResolvedCarriesWhoClosedItAndWhatItWasBefore() {
        var closed = open.closeByDeadline(OPENED_AT.plusHours(49));

        adapter.append(ReportResolved.of(closed, ReportStatus.OPEN, ReportClosedBy.DEADLINE, reservation, EVENT_ID));

        EventContract.assertSameShape(written().getPayload(), "ReportResolved");
        assertThat(written().getPayload()).contains("\"closedBy\":\"DEADLINE\"", "\"previousStatus\":\"OPEN\"",
                "\"resolution\":\"CONSUMER_FAVOR\"");
        assertThat(ReportResolution.valueOf("CONSUMER_FAVOR")).isNotNull();
    }

    @Test
    void theCorrelationIdOfTheRequestTravelsInTheEnvelope() {
        MDC.put("correlationId", "req-42");
        try {
            adapter.append((DomainEvent) ReportOpened.of(open, reservation, EVENT_ID));
        } finally {
            MDC.remove("correlationId");
        }

        assertThat(written().getPayload()).contains("\"correlationId\":\"req-42\"");
    }

    private OutboxEventJpaEntity written() {
        var saved = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(saved.capture());
        return saved.getValue();
    }
}
