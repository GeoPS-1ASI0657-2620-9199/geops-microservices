package com.geopslabs.geops.catalog.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.catalog.infrastructure.messaging.UnreadableEventException;
import com.geopslabs.geops.catalog.application.services.CellInvalidator;
import com.geopslabs.geops.catalog.application.services.MerchantReputationService;
import com.geopslabs.geops.catalog.application.usecases.ApplyBusinessVerifiedUseCase;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.ReputationEventsPort;
import org.springframework.transaction.support.TransactionOperations;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.ExponentialBackOff;

import java.time.Clock;
import java.util.List;

/**
 * What happens to an event the consumer cannot apply (US33, S3-02), in three cases:
 * <ul>
 * <li>Unreadable (broken JSON, missing field, version newer than known): to {@code <topic>.DLT} at once.</li>
 * <li>The database or Redis is down: retried with no limit, 1 s doubling up to 60 s, and applied when the
 * dependency comes back. It is never discarded.</li>
 * <li>Any other failure, a rule or a bug: five attempts (1, 2, 4 and 8 s apart) and then to {@code <topic>.DLT}
 * with its cause.</li>
 * </ul>
 * Every event that goes to the dead letter topic is logged with its eventId; a script replays it later.
 */
@Configuration
public class MessagingConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagingConfiguration.class);
    static final String DLT_SUFFIX = ".DLT";
    private static final int RETRIES = 4;
    private static final long FIRST_RETRY_MS = 1_000;
    private static final double RETRY_MULTIPLIER = 2.0;
    private static final long MAX_INFRASTRUCTURE_RETRY_MS = 60_000;
    private static final String UNKNOWN = "unknown";
    private static final List<Class<? extends Exception>> INFRASTRUCTURE_FAILURES = List.of(
            CannotCreateTransactionException.class, DataAccessResourceFailureException.class,
            TransientDataAccessException.class);
    /** Redis only exists in Catalog: it is compared by name so the other services do not need the dependency. */
    private static final String REDIS_FAILURE = "org.springframework.data.redis.RedisConnectionFailureException";

    @Bean
    public DefaultErrorHandler eventsErrorHandler(KafkaTemplate<String, String> template, ObjectMapper objectMapper,
                                                  @Value("${spring.application.name}") String service) {
        var deadLetter = new DeadLetterPublishingRecoverer(template,
                (record, exception) -> new TopicPartition(record.topic() + DLT_SUFFIX, record.partition()));
        ConsumerRecordRecoverer recoverer = (record, exception) -> {
            LOGGER.error("{}.event.dead-lettered eventId={} topic={} partition={} offset={} reason={}", service,
                    eventIdOf(record, objectMapper), record.topic(), record.partition(), record.offset(),
                    rootCause(exception).getClass().getSimpleName());
            deadLetter.accept(record, exception);
        };
        var attempts = new ExponentialBackOffWithMaxRetries(RETRIES);
        attempts.setInitialInterval(FIRST_RETRY_MS);
        attempts.setMultiplier(RETRY_MULTIPLIER);
        var handler = new DefaultErrorHandler(recoverer, attempts);
        handler.addNotRetryableExceptions(UnreadableEventException.class);
        handler.setBackOffFunction((record, exception) -> isInfrastructure(exception) ? retryForever() : null);
        handler.setCommitRecovered(true);
        return handler;
    }

    @Bean
    public ApplyBusinessVerifiedUseCase applyBusinessVerifiedUseCase(MerchantStandingRepositoryPort standings,
                                                                     ReputationEventsPort reputationEvents,
                                                                     CellInvalidator cellInvalidator,
                                                                     TransactionOperations transactions,
                                                                     Clock clock) {
        return new MerchantReputationService(standings, reputationEvents, cellInvalidator, transactions, clock);
    }

    static BackOff retryForever() {
        var backOff = new ExponentialBackOff(FIRST_RETRY_MS, RETRY_MULTIPLIER);
        backOff.setMaxInterval(MAX_INFRASTRUCTURE_RETRY_MS);
        backOff.setMaxElapsedTime(ExponentialBackOff.DEFAULT_MAX_ELAPSED_TIME);
        return backOff;
    }

    static boolean isInfrastructure(Throwable exception) {
        for (var cause = exception; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
            var failure = cause;
            if (INFRASTRUCTURE_FAILURES.stream().anyMatch(type -> type.isInstance(failure))
                    || REDIS_FAILURE.equals(failure.getClass().getName())) {
                return true;
            }
        }
        return false;
    }

    private static String eventIdOf(ConsumerRecord<?, ?> record, ObjectMapper objectMapper) {
        try {
            return objectMapper.readTree(String.valueOf(record.value())).path("eventId").asText(UNKNOWN);
        } catch (Exception exception) {
            return UNKNOWN;
        }
    }

    private static Throwable rootCause(Throwable exception) {
        var root = exception;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }
}
