package com.geopslabs.geops.reservation.acceptance;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;

@TestConfiguration
public class AcceptanceTestConfiguration {
    static final Instant ACCEPTANCE_NOW = Instant.parse("2026-10-08T13:05:00Z");

    @Bean
    @Primary
    public MutableClock acceptanceClock() {
        return new MutableClock(ACCEPTANCE_NOW);
    }

    @Bean
    @Primary
    public FakeOfferCatalog fakeOfferCatalog() {
        return new FakeOfferCatalog();
    }
}
