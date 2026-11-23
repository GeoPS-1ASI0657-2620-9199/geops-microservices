package com.geopslabs.geops.identity.configuration;

import com.geopslabs.geops.identity.domain.ports.TaxpayerRegistryPort;
import com.geopslabs.geops.identity.infrastructure.decolecta.DecolectaProperties;
import com.geopslabs.geops.identity.infrastructure.decolecta.DecolectaTaxpayerRegistryAdapter;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Where the RUC is looked up. geops.taxpayer-lookup.provider=registry (the default, used by the tests) reads the copy
 * of the SUNAT reduced registry; decolecta asks the online lookup for one RUC and turns off the daily registry
 * download. The circuit breaker "decolecta" comes from resilience4j.circuitbreaker.instances in application.yml.
 */
@Configuration
@EnableConfigurationProperties(DecolectaProperties.class)
public class TaxpayerLookupConfiguration {
    public static final String PROVIDER_PROPERTY = "geops.taxpayer-lookup.provider";
    public static final String DECOLECTA = "decolecta";
    public static final String REGISTRY = "registry";
    public static final String DECOLECTA_CIRCUIT_BREAKER = "decolecta";

    @Bean
    @ConditionalOnProperty(name = PROVIDER_PROPERTY, havingValue = DECOLECTA)
    public TaxpayerRegistryPort decolectaTaxpayerRegistry(DecolectaProperties properties,
                                                          CircuitBreakerRegistry circuitBreakers, Clock clock) {
        return new DecolectaTaxpayerRegistryAdapter(properties,
                circuitBreakers.circuitBreaker(DECOLECTA_CIRCUIT_BREAKER), clock);
    }
}
