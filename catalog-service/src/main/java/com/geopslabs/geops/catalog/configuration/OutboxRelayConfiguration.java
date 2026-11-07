package com.geopslabs.geops.catalog.configuration;

import com.geopslabs.geops.catalog.infrastructure.messaging.OutboxRelayProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OutboxRelayProperties.class)
@ConditionalOnProperty(name = "geops.outbox.relay-enabled", havingValue = "true")
public class OutboxRelayConfiguration {
}
