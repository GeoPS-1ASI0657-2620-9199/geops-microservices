package com.geopslabs.geops.catalog.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.catalog.application.services.CellInvalidator;
import com.geopslabs.geops.catalog.application.services.MerchantReputationService;
import com.geopslabs.geops.catalog.application.usecases.ApplyBusinessVerifiedUseCase;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.ReputationEventsPort;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.util.backoff.ExponentialBackOff;

import java.time.Clock;
import java.time.DateTimeException;

/**
 * E2-03 split in two. An event that cannot be read (bad JSON, missing fields, wrong id or date) is never going to
 * work: it is logged with its eventId and acknowledged at once, so it does not stop the consumer. Any other failure
 * (database or Redis down, timeouts) is transient: the event is retried with a backoff of 1 s doubling up to 60 s,
 * with no limit, and is applied when the dependency comes back; it is never discarded. The dead letter topic
 * arrives with iteration 4.
 */
@Configuration
public class MessagingConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagingConfiguration.class);
    private static final long FIRST_RETRY_MS = 1_000;
    private static final double RETRY_MULTIPLIER = 2.0;
    private static final long MAX_RETRY_INTERVAL_MS = 60_000;
    private static final String UNKNOWN = "unknown";

    @Bean
    public DefaultErrorHandler identityEventsErrorHandler(ObjectMapper objectMapper) {
        var handler = new DefaultErrorHandler((record, exception) -> LOGGER.error(
                "catalog.event.discarded eventId={} topic={} partition={} offset={} reason={} (unreadable)",
                eventIdOf(record, objectMapper), record.topic(), record.partition(), record.offset(),
                rootCause(exception).getClass().getSimpleName()),
                retryForever());
        handler.addNotRetryableExceptions(JsonProcessingException.class, IllegalArgumentException.class,
                DateTimeException.class, NullPointerException.class);
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

    static ExponentialBackOff retryForever() {
        var backOff = new ExponentialBackOff(FIRST_RETRY_MS, RETRY_MULTIPLIER);
        backOff.setMaxInterval(MAX_RETRY_INTERVAL_MS);
        backOff.setMaxElapsedTime(ExponentialBackOff.DEFAULT_MAX_ELAPSED_TIME);
        return backOff;
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
