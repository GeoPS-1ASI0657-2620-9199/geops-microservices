package com.geopslabs.geops.identity.configuration;

import com.geopslabs.geops.identity.domain.ports.GeocodingPort;
import com.geopslabs.geops.identity.infrastructure.geocoding.NominatimGeocodingAdapter;
import com.geopslabs.geops.identity.infrastructure.geocoding.NominatimProperties;
import com.geopslabs.geops.identity.infrastructure.geocoding.NominatimRateLimiter;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The circuit breaker "nominatim" comes from resilience4j.circuitbreaker.instances.nominatim in application.yml.
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
}
