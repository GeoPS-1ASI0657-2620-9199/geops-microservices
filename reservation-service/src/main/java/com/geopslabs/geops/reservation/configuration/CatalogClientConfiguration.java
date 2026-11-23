package com.geopslabs.geops.reservation.configuration;

import com.geopslabs.geops.reservation.infrastructure.catalog.CatalogOfferCatalogAdapter;
import com.geopslabs.geops.reservation.infrastructure.resilience.CircuitBreakerTransitionLogger;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Catalog behind OfferCatalogPort (US34). The circuit breaker and the bulkhead "catalog" come from
 * resilience4j.circuitbreaker.instances.catalog and resilience4j.bulkhead.instances.catalog in application.yml.
 */
@Configuration
public class CatalogClientConfiguration {
    public static final String CATALOG = "catalog";

    @Bean
    public CatalogOfferCatalogAdapter catalogOfferCatalogAdapter(RestClient.Builder builder,
                                                                 @Value("${geops.catalog.base-url}") String baseUrl,
                                                                 CircuitBreakerRegistry circuitBreakers,
                                                                 BulkheadRegistry bulkheads) {
        return new CatalogOfferCatalogAdapter(builder, baseUrl, circuitBreakers.circuitBreaker(CATALOG),
                bulkheads.bulkhead(CATALOG));
    }

    /** Every circuit breaker of the registry logs its state changes. */
    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerTransitionLogger() {
        return new CircuitBreakerTransitionLogger();
    }
}
