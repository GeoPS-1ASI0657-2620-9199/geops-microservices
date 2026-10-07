package com.geopslabs.geops.notification.acceptance;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@TestConfiguration
public class AcceptanceTestConfiguration {
    static final Instant ACCEPTANCE_NOW = Instant.parse("2026-10-08T13:05:00Z");

    @Bean
    @Primary
    public Clock acceptanceClock() {
        return Clock.fixed(ACCEPTANCE_NOW, ZoneOffset.UTC);
    }
}
