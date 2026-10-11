package com.geopslabs.geops.reservation.infrastructure.messaging;

import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxEventJpaEntity;
import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxEventJpaRepository;
import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxEventStatus;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The relay against a real PostgreSQL: the row locks, the status filter and the order of the outbox. Kafka is a
 * mock so each test decides which row it confirms or rejects.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "geops.outbox.relay-enabled=false")
class OutboxRelayIntegrationTest {
    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres");
    private static final String TOPIC = "reservation.events";
    private static final int MAX_ATTEMPTS = 3;
    private static final long SEND_TIMEOUT_MS = 2_000;
    private static final Long POISON_AGGREGATE = 1L;
    private static final Long NEXT_AGGREGATE = 2L;

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE);

    static {
        POSTGRES.start();
    }

    @Autowired
    private OutboxEventJpaRepository outbox;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

    @BeforeEach
    void emptyOutbox() {
        outbox.deleteAll();
    }

    @Test
    void aRowKafkaAlwaysRejectsEndsFailedAndTheNextOneIsPublished() {
        var poison = store(POISON_AGGREGATE, Instant.parse("2026-10-20T17:00:00Z"));
        var next = store(NEXT_AGGREGATE, Instant.parse("2026-10-20T17:00:01Z"));
        when(kafkaTemplate.send(eq(TOPIC), eq("1"), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RecordTooLargeException("record too large")));
        when(kafkaTemplate.send(eq(TOPIC), eq("2"), anyString())).thenReturn(confirmed());
        var relay = relay(50);

        for (int round = 1; round < MAX_ATTEMPTS; round++) {
            relay.relayPending();
            assertThat(reload(poison).getAttempts()).isEqualTo(round);
            assertThat(reload(poison).getStatus()).isEqualTo(OutboxEventStatus.PENDING);
            assertThat(reload(next).getPublishedAt()).as("the order is kept while the row is retried").isNull();
        }
        relay.relayPending();

        var failed = reload(poison);
        assertThat(failed.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(failed.getAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(failed.getLastError()).contains("record too large");
        assertThat(failed.getPublishedAt()).isNull();
        assertThat(reload(next).getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(reload(next).getPublishedAt()).isNotNull();

        relay.relayPending();
        assertThat(reload(poison).getAttempts()).as("a FAILED row is not read again").isEqualTo(MAX_ATTEMPTS);
    }

    private OutboxRelay relay(int batchSize) {
        return new OutboxRelay(outbox, kafkaTemplate, new TransactionTemplate(transactionManager), Clock.systemUTC(),
                new OutboxRelayProperties(TOPIC, batchSize, SEND_TIMEOUT_MS, MAX_ATTEMPTS));
    }

    private UUID store(Long aggregateId, Instant createdAt) {
        var row = new OutboxEventJpaEntity();
        row.setId(UUID.randomUUID());
        row.setAggregateId(aggregateId);
        row.setEventType("ReservationRedeemed");
        row.setPayload("{\"aggregateId\": " + aggregateId + "}");
        row.setCreatedAt(createdAt);
        return outbox.save(row).getId();
    }

    private OutboxEventJpaEntity reload(UUID id) {
        return outbox.findById(id).orElseThrow();
    }

    private static CompletableFuture<SendResult<String, String>> confirmed() {
        return CompletableFuture.completedFuture(null);
    }
}
