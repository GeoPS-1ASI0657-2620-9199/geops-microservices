package com.geopslabs.geops.notification.configuration;

import com.geopslabs.geops.notification.infrastructure.resilience.CircuitBreakerTransitionLogger;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** US34: the circuit breakers "fcm" and "smtp" log every state change. */
@Configuration
public class ResilienceConfiguration {
    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerTransitionLogger() {
        return new CircuitBreakerTransitionLogger();
    }
}
