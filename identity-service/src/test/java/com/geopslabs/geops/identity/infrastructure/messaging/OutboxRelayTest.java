package com.geopslabs.geops.identity.infrastructure.messaging;

import com.geopslabs.geops.identity.infrastructure.persistence.OutboxEventJpaEntity;
import com.geopslabs.geops.identity.infrastructure.persistence.OutboxEventJpaRepository;
import com.geopslabs.geops.identity.infrastructure.persistence.OutboxEventStatus;
import org.apache.kafka.common.errors.RecordTooLargeException;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {
    private static final Instant NOW = Instant.parse("2026-10-20T17:42:11Z");
    private static final String TOPIC = "identity.events";
    private static final int BATCH_SIZE = 50;
    private static final long SEND_TIMEOUT_MS = 50;
    private static final int MAX_ATTEMPTS = 3;

    @Mock
    private OutboxEventJpaRepository repository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(repository, kafkaTemplate, TransactionOperations.withoutTransaction(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                new OutboxRelayProperties(TOPIC, BATCH_SIZE, SEND_TIMEOUT_MS, MAX_ATTEMPTS));
        lenient().when(repository.tryAdvisoryLock(OutboxRelay.RELAY_LOCK)).thenReturn(true);
    }

    @Test
    void doesNothingWhenAnotherReplicaHoldsTheLock() {
        when(repository.tryAdvisoryLock(OutboxRelay.RELAY_LOCK)).thenReturn(false);

        relay.relayPending();

        verify(repository, never()).lockPending(anyInt());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void sendsEachRowWithItsAggregateAsKeyAndMarksItAfterTheConfirmation() {
        var first = row(5012L);
        var second = row(5013L);
        when(repository.lockPending(BATCH_SIZE)).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(TOPIC, "5012", first.getPayload())).thenReturn(confirmed());
        when(kafkaTemplate.send(TOPIC, "5013", second.getPayload())).thenReturn(confirmed());

        relay.relayPending();

        assertThat(first.getPublishedAt()).isEqualTo(NOW);
        assertThat(first.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(second.getPublishedAt()).isEqualTo(NOW);
    }

    @Test
    void countsARejectionAndStopsTheRoundToKeepTheOrder() {
        var first = row(5012L);
        var second = row(5013L);
        when(repository.lockPending(BATCH_SIZE)).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(TOPIC, "5012", first.getPayload())).thenReturn(rejected());

        relay.relayPending();

        assertThat(first.getPublishedAt()).isNull();
        assertThat(first.getAttempts()).isEqualTo(1);
        assertThat(first.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(first.getLastError()).startsWith("ExecutionException: ").contains("too large");
        verify(kafkaTemplate, never()).send(TOPIC, "5013", second.getPayload());
    }

    @Test
    void marksTheRowFailedAtTheMaximumAttemptsAndPublishesTheNextOne() {
        var poison = row(5012L);
        poison.setAttempts(MAX_ATTEMPTS - 1);
        var next = row(5013L);
        when(repository.lockPending(BATCH_SIZE)).thenReturn(List.of(poison, next));
        when(kafkaTemplate.send(TOPIC, "5012", poison.getPayload())).thenReturn(rejected());
        when(kafkaTemplate.send(TOPIC, "5013", next.getPayload())).thenReturn(confirmed());

        relay.relayPending();

        assertThat(poison.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(poison.getAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(poison.getPublishedAt()).isNull();
        assertThat(next.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
    }

    @Test
    void aKafkaTimeoutKeepsTheReasonButDoesNotCountAnAttempt() {
        var first = row(5012L);
        first.setAttempts(MAX_ATTEMPTS - 1);
        var second = row(5013L);
        when(repository.lockPending(BATCH_SIZE)).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(TOPIC, "5012", first.getPayload())).thenReturn(new CompletableFuture<>());

        relay.relayPending();
        relay.relayPending();
        relay.relayPending();

        assertThat(first.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(first.getAttempts()).as("timeouts do not count towards FAILED").isEqualTo(MAX_ATTEMPTS - 1);
        assertThat(first.getLastError()).startsWith("TimeoutException");
        verify(kafkaTemplate, never()).send(TOPIC, "5013", second.getPayload());
    }

    @Test
    void afterAnOutageOneRejectionDoesNotFailARowThatWasNeverRejectedBefore() {
        var row = row(5012L);
        when(repository.lockPending(BATCH_SIZE)).thenReturn(List.of(row));
        when(kafkaTemplate.send(TOPIC, "5012", row.getPayload()))
                .thenReturn(new CompletableFuture<>(), new CompletableFuture<>(), new CompletableFuture<>(),
                        new CompletableFuture<>(), rejected());

        for (int round = 0; round < 5; round++) {
            relay.relayPending();
        }

        assertThat(row.getAttempts()).isEqualTo(1);
        assertThat(row.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    }

    @Test
    void treatsARetriableKafkaErrorAsKafkaUnavailable() {
        var first = row(5012L);
        first.setAttempts(MAX_ATTEMPTS);
        when(repository.lockPending(BATCH_SIZE)).thenReturn(List.of(first));
        when(kafkaTemplate.send(TOPIC, "5012", first.getPayload())).thenThrow(new org.springframework.kafka
                .KafkaException("send failed", new org.apache.kafka.common.errors.TimeoutException("no metadata")));

        relay.relayPending();

        assertThat(first.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(first.getAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(first.getLastError()).contains("no metadata");
    }

    @Test
    void keepsWhatWasConfirmedBeforeAFailure() {
        var first = row(5012L);
        var second = row(5013L);
        when(repository.lockPending(BATCH_SIZE)).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(TOPIC, "5012", first.getPayload())).thenReturn(confirmed());
        when(kafkaTemplate.send(TOPIC, "5013", second.getPayload())).thenReturn(rejected());

        relay.relayPending();

        assertThat(first.getPublishedAt()).isEqualTo(NOW);
        assertThat(second.getPublishedAt()).isNull();
    }

    @Test
    void doesNothingWithAnEmptyOutbox() {
        when(repository.lockPending(BATCH_SIZE)).thenReturn(List.of());

        relay.relayPending();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    private static CompletableFuture<SendResult<String, String>> confirmed() {
        return CompletableFuture.completedFuture(null);
    }

    private static CompletableFuture<SendResult<String, String>> rejected() {
        return CompletableFuture.failedFuture(new RecordTooLargeException("record too large"));
    }

    private static OutboxEventJpaEntity row(Long aggregateId) {
        var row = new OutboxEventJpaEntity();
        row.setId(UUID.randomUUID());
        row.setAggregateId(aggregateId);
        row.setEventType("BusinessVerified");
        row.setPayload("{\"aggregateId\":" + aggregateId + "}");
        row.setCreatedAt(NOW.minusSeconds(1));
        return row;
    }
}
