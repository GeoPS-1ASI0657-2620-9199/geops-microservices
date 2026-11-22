package com.geopslabs.geops.reservation.infrastructure.messaging;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.geopslabs.geops.reservation.domain.events.ReservationCreated;
import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.ReservationCode;
import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxEventJpaEntity;
import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxEventJpaRepository;
import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxJpaAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** ReservationCreated has the shape of the example Catalog builds the campaign metrics on (US11). */
class ReservationCreatedContractTest {
    private static final LocalDateTime RESERVED_AT = LocalDateTime.parse("2026-10-20T15:05:00");
    private static final UUID EVENT_ID = UUID.fromString("3c2a7d4e-91b0-4f6e-8d25-6a1f0b9c4e72");

    private final OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
    private final OutboxJpaAdapter adapter = new OutboxJpaAdapter(repository, Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build(),
            Clock.fixed(Instant.parse("2026-10-20T15:05:00Z"), ZoneOffset.UTC));

    @Test
    void reservationCreatedHasTheShapeOfTheExample() {
        var reservation = new Reservation(5012L, new ReservationCode("Q4ZT8K2M"), 2001L, 1052L, 84L,
                "2x1 en almuerzos ejecutivos", RESERVED_AT, LocalDateTime.parse("2026-10-21T04:59:59"), null,
                ReservationStatus.ACTIVE);

        adapter.append(ReservationCreated.of(reservation, EVENT_ID));

        var saved = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(saved.capture());
        EventContract.assertSameShape(saved.getValue().getPayload(), "ReservationCreated");
        assertThat(saved.getValue().getAggregateId()).isEqualTo(5012L);
        assertThat(saved.getValue().getPayload()).contains("\"occurredAt\":\"2026-10-20T15:05:00Z\"",
                "\"offerId\":1052", "\"expiresAt\":\"2026-10-21T04:59:59Z\"");
    }
}
