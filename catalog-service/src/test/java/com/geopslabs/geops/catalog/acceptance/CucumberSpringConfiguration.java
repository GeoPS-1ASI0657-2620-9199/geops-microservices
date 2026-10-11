package com.geopslabs.geops.catalog.acceptance;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AcceptanceTestConfiguration.class)
public class CucumberSpringConfiguration {
    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres");
    private static final int REDIS_PORT = 6379;

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE)
            .withInitScript("acceptance/enable-postgis.sql");

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(REDIS_PORT);

    @ServiceConnection
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));

    /** Nominatim: no scenario reaches the real service (E2-15). */
    static final WireMockServer NOMINATIM = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        POSTGRES.start();
        REDIS.start();
        KAFKA.start();
        NOMINATIM.start();
    }

    @DynamicPropertySource
    static void acceptanceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", TestIdentity::jwksUri);
        // The scenarios run the jobs themselves, at the date they set.
        registry.add("geops.jobs.enabled", () -> "false");
        // Each scenario that needs Redis gets it at once, even right after the one that took it down.
        registry.add("geops.cache.pause-after-failure", () -> "0s");
        registry.add("geops.nominatim.base-url", NOMINATIM::baseUrl);
        registry.add("geops.nominatim.min-interval", () -> "0s");
    }
}
