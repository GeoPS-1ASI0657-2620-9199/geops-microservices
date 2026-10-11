package com.geopslabs.geops.catalog.configuration;

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
import org.springframework.util.backoff.FixedBackOff;

import java.time.Clock;

/**
 * E2-03: an event that cannot be processed is retried three times one second apart, then logged with its eventId and
 * acknowledged, so one bad event never stops the consumer. The dead letter topic arrives with iteration 4.
 */
@Configuration
public class MessagingConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagingConfiguration.class);
    private static final long RETRY_INTERVAL_MS = 1_000;
    private static final long RETRIES = 3;
    private static final String UNKNOWN = "unknown";

    @Bean
    public DefaultErrorHandler identityEventsErrorHandler(ObjectMapper objectMapper) {
        var handler = new DefaultErrorHandler((record, exception) -> LOGGER.error(
                "catalog.event.discarded eventId={} topic={} partition={} offset={} reason={}",
                eventIdOf(record, objectMapper), record.topic(), record.partition(), record.offset(),
                rootCause(exception).getClass().getSimpleName()),
                new FixedBackOff(RETRY_INTERVAL_MS, RETRIES));
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
