package com.geopslabs.geops.reservation.infrastructure.messaging;

import com.geopslabs.geops.reservation.domain.events.ReservationRedeemed;
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

/** ReservationRedeemed has the shape of its example, the one Catalog (ranking) and Engagement (reviews) read. */
class ReservationRedeemedContractTest {
    private static final LocalDateTime REDEEMED_AT = LocalDateTime.parse("2026-10-20T17:42:10");

    private final OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
    private final OutboxJpaAdapter adapter = new OutboxJpaAdapter(repository, Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build(),
            Clock.fixed(Instant.parse("2026-10-20T17:42:10Z"), ZoneOffset.UTC));

    @Test
    void reservationRedeemedHasTheShapeOfTheExample() {
        var redeemed = new Reservation(5012L, new ReservationCode("Q4ZT8K2M"), 2001L, 1052L, 84L,
                "2x1 en almuerzos ejecutivos", REDEEMED_AT.minusHours(3), REDEEMED_AT.plusDays(1), REDEEMED_AT,
                ReservationStatus.REDEEMED);

        adapter.append(ReservationRedeemed.of(redeemed, UUID.randomUUID()));

        EventContract.assertSameShape(written().getPayload(), "ReservationRedeemed");
        assertThat(written().getAggregateId()).isEqualTo(5012L);
        assertThat(written().getPayload()).contains("\"redeemedAt\":\"2026-10-20T17:42:10Z\"",
                "\"occurredAt\":\"2026-10-20T17:42:10Z\"", "\"businessId\":84", "\"consumerId\":2001");
    }

    private OutboxEventJpaEntity written() {
        var saved = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(saved.capture());
        return saved.getValue();
    }
}
