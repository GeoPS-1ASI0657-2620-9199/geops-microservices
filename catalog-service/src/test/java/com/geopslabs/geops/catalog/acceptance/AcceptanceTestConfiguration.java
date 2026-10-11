package com.geopslabs.geops.catalog.acceptance;

import com.geopslabs.geops.catalog.application.services.OfferQueryService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;

import java.time.Instant;

@TestConfiguration
public class AcceptanceTestConfiguration {
    static final Instant ACCEPTANCE_NOW = Instant.parse("2026-10-08T13:05:00Z");

    @Bean
    @Primary
    public MutableClock acceptanceClock() {
        return new MutableClock(ACCEPTANCE_NOW, OfferQueryService.OFFER_ZONE);
    }

    /** In the compose the topic comes from kafka-init (E2-02); here KafkaAdmin creates it at startup. */
    @Bean
    public NewTopic identityEvents() {
        return TopicBuilder.name(IdentityEventSteps.TOPIC).partitions(3).replicas(1).build();
    }
}
