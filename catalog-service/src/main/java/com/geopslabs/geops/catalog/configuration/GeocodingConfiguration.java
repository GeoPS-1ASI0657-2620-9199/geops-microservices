package com.geopslabs.geops.catalog.configuration;

import com.geopslabs.geops.catalog.application.services.CellInvalidator;
import com.geopslabs.geops.catalog.application.services.GeocodingRunScheduler;
import com.geopslabs.geops.catalog.application.services.GeocodingRunService;
import com.geopslabs.geops.catalog.domain.ports.GeocodingPort;
import com.geopslabs.geops.catalog.domain.ports.JobLockPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import com.geopslabs.geops.catalog.infrastructure.geocoding.NominatimGeocodingAdapter;
import com.geopslabs.geops.catalog.infrastructure.geocoding.NominatimProperties;
import com.geopslabs.geops.catalog.infrastructure.geocoding.NominatimRateLimiter;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;

/**
 * Nominatim behind GeocodingPort, the same design as Identity (S2-03, E2-14, E2-15). The circuit breaker "nominatim"
 * comes from resilience4j.circuitbreaker.instances.nominatim in application.yml.
 */
@Configuration
@EnableConfigurationProperties(NominatimProperties.class)
public class GeocodingConfiguration {
    public static final String NOMINATIM_CIRCUIT_BREAKER = "nominatim";

    @Bean
    public GeocodingPort geocodingPort(NominatimProperties properties, CircuitBreakerRegistry circuitBreakers) {
        return new NominatimGeocodingAdapter(properties, circuitBreakers.circuitBreaker(NOMINATIM_CIRCUIT_BREAKER),
                new NominatimRateLimiter(properties.minInterval()));
    }

    @Bean
    public GeocodingRunService geocodingRunService(OfferRepositoryPort offerRepository, GeocodingPort geocoding,
                                                   JobLockPort jobLock, CellInvalidator cellInvalidator,
                                                   TransactionOperations transactions, Clock clock) {
        return new GeocodingRunService(offerRepository, geocoding, jobLock, cellInvalidator, transactions, clock);
    }

    @Bean
    public GeocodingRunScheduler geocodingRunScheduler(GeocodingRunService geocodingRunService) {
        return new GeocodingRunScheduler(geocodingRunService);
    }
}
