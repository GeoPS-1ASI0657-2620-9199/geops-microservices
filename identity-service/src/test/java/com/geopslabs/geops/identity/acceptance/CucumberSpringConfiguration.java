package com.geopslabs.geops.identity.acceptance;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * PostgreSQL in a container; Nominatim and the SUNAT download are one WireMock server, so no scenario reaches a real
 * provider (QAS10). The registry is loaded by the scenarios that need it and the relay stays off: the scenarios
 * check what is written to the outbox.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"geops.sunat.schedule-enabled=false", "geops.outbox.relay-enabled=false",
                "geops.verification.retry-enabled=false", "geops.nominatim.min-interval=0s"})
public class CucumberSpringConfiguration {
    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres");

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE);

    static final WireMockServer PROVIDERS = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        POSTGRES.start();
        PROVIDERS.start();
    }

    @DynamicPropertySource
    static void jwtKeys(DynamicPropertyRegistry registry) {
        TestKeys.register(registry);
    }

    @DynamicPropertySource
    static void providers(DynamicPropertyRegistry registry) {
        registry.add("geops.nominatim.base-url", PROVIDERS::baseUrl);
    }
}
