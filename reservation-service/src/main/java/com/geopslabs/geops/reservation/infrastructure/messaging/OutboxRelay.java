package com.geopslabs.geops.reservation.infrastructure.messaging;

import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxEventJpaEntity;
import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxEventJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes the outbox to Kafka, at least once. A row is marked only after Kafka confirms it; the first failure
 * ends the round and the remaining rows wait for the next one. The redemption never waits for this process.
 */
@Component
@ConditionalOnProperty(name = "geops.outbox.relay-enabled", havingValue = "true")
public class OutboxRelay {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionOperations transactions;
    private final Clock clock;
    private final String topic;
    private final int batchSize;
    private final long sendTimeoutMs;

    @SuppressWarnings("java:S107")
    public OutboxRelay(OutboxEventJpaRepository repository, KafkaTemplate<String, String> kafkaTemplate,
                       TransactionOperations transactions, Clock clock,
                       @Value("${geops.outbox.topic}") String topic,
                       @Value("${geops.outbox.batch-size}") int batchSize,
                       @Value("${geops.outbox.send-timeout-ms}") long sendTimeoutMs) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.transactions = transactions;
        this.clock = clock;
        this.topic = topic;
        this.batchSize = batchSize;
        this.sendTimeoutMs = sendTimeoutMs;
    }

    @Scheduled(fixedDelayString = "${geops.outbox.relay-delay-ms:1000}")
    public void relayPending() {
        var published = transactions.execute(status -> publishLockedRows());
        if (published != null && published > 0) {
            LOGGER.info("outbox.relay.published count={} topic={}", published, topic);
        }
    }

    private int publishLockedRows() {
        var published = 0;
        for (var row : repository.lockUnpublished(batchSize)) {
            if (!send(row)) {
                break;
            }
            row.setPublishedAt(clock.instant());
            published++;
        }
        return published;
    }

    private boolean send(OutboxEventJpaEntity row) {
        try {
            kafkaTemplate.send(topic, String.valueOf(row.getAggregateId()), row.getPayload())
                    .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logFailure(row, exception);
            return false;
        } catch (ExecutionException | TimeoutException | RuntimeException exception) {
            logFailure(row, exception);
            return false;
        }
    }

    private void logFailure(OutboxEventJpaEntity row, Exception exception) {
        LOGGER.warn("outbox.relay.failed eventId={} eventType={} topic={} reason={}", row.getId(),
                row.getEventType(), topic, exception.getClass().getSimpleName());
    }
}
