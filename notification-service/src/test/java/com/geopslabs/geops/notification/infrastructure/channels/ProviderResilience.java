package com.geopslabs.geops.notification.infrastructure.channels;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import java.time.Duration;

/** Same values as resilience4j.circuitbreaker.configs.provider in application.yml: five failures in ten seconds. */
final class ProviderResilience {
    static final int FAILURES_THAT_OPEN = 5;

    private ProviderResilience() {
    }

    static CircuitBreaker circuitBreaker(String name) {
        return CircuitBreaker.of(name, CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(FAILURES_THAT_OPEN)
                .failureRateThreshold(100)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(1)
                .ignoreExceptions(BulkheadFullException.class)
                .build());
    }

    /** One place only, so a test can fill it. */
    static Bulkhead singlePlaceBulkhead(String name) {
        return Bulkhead.of(name, BulkheadConfig.custom().maxConcurrentCalls(1).maxWaitDuration(Duration.ZERO).build());
    }
}
