package com.geopslabs.geops.identity;

import com.geopslabs.geops.identity.acceptance.TestKeys;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "geops.outbox.relay-enabled=false")
class IdentityServiceApplicationTests {
    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres");
    private static final String LATEST_VERSION = "3";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE);

    static {
        POSTGRES.start();
    }

    @Autowired
    private Flyway flyway;
    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    @DynamicPropertySource
    static void jwtKeys(DynamicPropertyRegistry registry) {
        TestKeys.register(registry);
    }

    @Test
    void appliesEveryMigration() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo(LATEST_VERSION);
    }

    @Test
    void nominatimCircuitBreakerOpensWithFiveFailuresInTenSecondsForThirtySeconds() {
        var config = circuitBreakers.circuitBreaker("nominatim").getCircuitBreakerConfig();

        assertThat(config.getSlidingWindowType()).isEqualTo(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);
        assertThat(config.getSlidingWindowSize()).isEqualTo(10);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(config.getFailureRateThreshold()).isEqualTo(100f);
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(30).toMillis());
    }
}
