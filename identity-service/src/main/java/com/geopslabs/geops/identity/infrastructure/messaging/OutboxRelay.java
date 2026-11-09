package com.geopslabs.geops.identity.infrastructure.messaging;

import com.geopslabs.geops.identity.infrastructure.persistence.OutboxEventJpaEntity;
import com.geopslabs.geops.identity.infrastructure.persistence.OutboxEventJpaRepository;
import org.apache.kafka.common.errors.RetriableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes Identity's outbox to identity.events, at least once; the same design as Reservation's relay. A row is
 * marked only after Kafka confirms it. When Kafka does not answer (timeout or retriable error) the round stops, the
 * reason is kept on the row and its attempts are not counted, so an outage of any length never pushes a sound row
 * towards FAILED. When Kafka answers and rejects a row, the rejection is counted and the round also stops to keep
 * the order, until the row reaches the maximum attempts: then it is marked FAILED and the round goes on with the
 * rest. Each round first takes a PostgreSQL advisory lock, so with two replicas only one publishes at a time and
 * the rows leave in seq order. The verification never waits for this process.
 */
@Component
@ConditionalOnProperty(name = "geops.outbox.relay-enabled", havingValue = "true")
public class OutboxRelay {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxRelay.class);
    private static final String REASON = "%s: %s";
    static final String RELAY_LOCK = "identity.outbox-relay";

    private final OutboxEventJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionOperations transactions;
    private final Clock clock;
    private final OutboxRelayProperties properties;

    public OutboxRelay(OutboxEventJpaRepository repository, KafkaTemplate<String, String> kafkaTemplate,
                       TransactionOperations transactions, Clock clock, OutboxRelayProperties properties) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.transactions = transactions;
        this.clock = clock;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${geops.outbox.relay-delay-ms:1000}")
    public void relayPending() {
        var published = transactions.execute(status -> publishLockedRows());
        if (published != null && published > 0) {
            LOGGER.info("outbox.relay.published count={} topic={}", published, properties.topic());
        }
    }

    private int publishLockedRows() {
        if (!repository.tryAdvisoryLock(RELAY_LOCK)) {
            LOGGER.debug("outbox.relay.skipped reason=another-replica-holds-the-lock");
            return 0;
        }
        var published = 0;
        for (var row : repository.lockPending(properties.batchSize())) {
            var failure = send(row);
            if (failure.isEmpty()) {
                row.markPublished(clock.instant());
                published++;
                continue;
            }
            var reason = REASON.formatted(failure.get().getClass().getSimpleName(), rootMessage(failure.get()));
            if (isKafkaUnavailable(failure.get())) {
                row.recordUnavailable(reason);
                LOGGER.warn("outbox.relay.kafka-unavailable eventId={} eventType={} attempts={} reason={}",
                        row.getId(), row.getEventType(), row.getAttempts(), reason);
                break;
            }
            row.recordRejection(reason);
            if (row.getAttempts() < properties.maxAttempts()) {
                LOGGER.warn("outbox.relay.rejected eventId={} eventType={} attempts={} reason={}", row.getId(),
                        row.getEventType(), row.getAttempts(), reason);
                break;
            }
            row.markFailed();
            LOGGER.error("outbox.relay.gave-up eventId={} eventType={} aggregateId={} attempts={} reason={}",
                    row.getId(), row.getEventType(), row.getAggregateId(), row.getAttempts(), row.getLastError());
        }
        return published;
    }

    private Optional<Exception> send(OutboxEventJpaEntity row) {
        try {
            kafkaTemplate.send(properties.topic(), String.valueOf(row.getAggregateId()), row.getPayload())
                    .get(properties.sendTimeoutMs(), TimeUnit.MILLISECONDS);
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.of(exception);
        } catch (Exception exception) {
            return Optional.of(exception);
        }
    }

    /** No confirmation in time, an interrupted wait or a Kafka error flagged as retriable. */
    static boolean isKafkaUnavailable(Throwable failure) {
        for (var cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof TimeoutException || cause instanceof InterruptedException
                    || cause instanceof RetriableException) {
                return true;
            }
        }
        return false;
    }

    private static String rootMessage(Throwable failure) {
        var root = failure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
