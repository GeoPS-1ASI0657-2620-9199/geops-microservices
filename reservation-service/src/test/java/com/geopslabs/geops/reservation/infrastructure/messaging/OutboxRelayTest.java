package com.geopslabs.geops.reservation.infrastructure.messaging;

import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxEventJpaEntity;
import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxEventJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {
    private static final Instant NOW = Instant.parse("2026-10-20T17:42:11Z");
    private static final String TOPIC = "reservation.events";
    private static final int BATCH_SIZE = 100;
    private static final long SEND_TIMEOUT_MS = 50;

    @Mock
    private OutboxEventJpaRepository repository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(repository, kafkaTemplate, TransactionOperations.withoutTransaction(),
                Clock.fixed(NOW, ZoneOffset.UTC), TOPIC, BATCH_SIZE, SEND_TIMEOUT_MS);
    }

    @Test
    void sendsEachRowWithItsAggregateAsKeyAndMarksItAfterTheConfirmation() {
        var first = row(5012L, "{\"eventId\":\"a\"}");
        var second = row(5013L, "{\"eventId\":\"b\"}");
        when(repository.lockUnpublished(BATCH_SIZE)).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(TOPIC, "5012", first.getPayload())).thenReturn(confirmed());
        when(kafkaTemplate.send(TOPIC, "5013", second.getPayload())).thenReturn(confirmed());

        relay.relayPending();

        assertThat(first.getPublishedAt()).isEqualTo(NOW);
        assertThat(second.getPublishedAt()).isEqualTo(NOW);
    }

    @Test
    void leavesTheRowUnmarkedWhenKafkaFailsAndStopsTheRound() {
        var first = row(5012L, "{\"eventId\":\"a\"}");
        var second = row(5013L, "{\"eventId\":\"b\"}");
        when(repository.lockUnpublished(BATCH_SIZE)).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(TOPIC, "5012", first.getPayload()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));

        relay.relayPending();

        assertThat(first.getPublishedAt()).isNull();
        assertThat(second.getPublishedAt()).isNull();
        verify(kafkaTemplate, never()).send(TOPIC, "5013", second.getPayload());
    }

    @Test
    void leavesTheRowUnmarkedWhenKafkaDoesNotConfirmInTime() {
        var first = row(5012L, "{\"eventId\":\"a\"}");
        when(repository.lockUnpublished(BATCH_SIZE)).thenReturn(List.of(first));
        when(kafkaTemplate.send(TOPIC, "5012", first.getPayload())).thenReturn(new CompletableFuture<>());

        relay.relayPending();

        assertThat(first.getPublishedAt()).isNull();
    }

    @Test
    void keepsWhatWasConfirmedBeforeAFailure() {
        var first = row(5012L, "{\"eventId\":\"a\"}");
        var second = row(5013L, "{\"eventId\":\"b\"}");
        when(repository.lockUnpublished(BATCH_SIZE)).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(TOPIC, "5012", first.getPayload())).thenReturn(confirmed());
        when(kafkaTemplate.send(TOPIC, "5013", second.getPayload()))
                .thenThrow(new IllegalStateException("metadata not available"));

        relay.relayPending();

        assertThat(first.getPublishedAt()).isEqualTo(NOW);
        assertThat(second.getPublishedAt()).isNull();
    }

    @Test
    void doesNothingWithAnEmptyOutbox() {
        when(repository.lockUnpublished(BATCH_SIZE)).thenReturn(List.of());

        relay.relayPending();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    private static CompletableFuture<SendResult<String, String>> confirmed() {
        return CompletableFuture.completedFuture(null);
    }

    private static OutboxEventJpaEntity row(Long aggregateId, String payload) {
        var row = new OutboxEventJpaEntity();
        row.setId(UUID.randomUUID());
        row.setAggregateId(aggregateId);
        row.setEventType("ReservationRedeemed");
        row.setPayload(payload);
        row.setCreatedAt(NOW.minusSeconds(1));
        return row;
    }
}
