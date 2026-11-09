package com.geopslabs.geops.identity.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * geops.outbox.*: topic, rows per round, wait for each Kafka confirmation and rejections before a row is FAILED.
 */
@ConfigurationProperties(prefix = "geops.outbox")
public record OutboxRelayProperties(String topic, int batchSize, long sendTimeoutMs, int maxAttempts) {
}
