package com.geopslabs.geops.identity.configuration;

import com.geopslabs.geops.identity.infrastructure.messaging.OutboxRelayProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The producer (acks=all, idempotence) is set in application.yml. With OUTBOX_RELAY_ENABLED=false nothing is
 * published and the events wait in outbox_events.
 */
@Configuration
@EnableConfigurationProperties(OutboxRelayProperties.class)
@ConditionalOnProperty(name = "geops.outbox.relay-enabled", havingValue = "true")
public class MessagingConfiguration {

    @Bean
    public TransactionOperations outboxRelayTransactions(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
