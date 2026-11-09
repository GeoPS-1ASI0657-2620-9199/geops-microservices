package com.geopslabs.geops.reservation.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * geops.outbox.*: topic, rows per round, wait for each Kafka confirmation and attempts before a row is FAILED.
 */
@ConfigurationProperties(prefix = "geops.outbox")
public record OutboxRelayProperties(String topic, int batchSize, long sendTimeoutMs, int maxAttempts) {
}
