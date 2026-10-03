package com.geopslabs.geops.identity;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IdentityServiceApplicationTests {
    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres");
    private static final int SECRET_BYTES = 48;
    private static final String BASELINE_VERSION = "1";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE);

    static {
        POSTGRES.start();
    }

    @Autowired
    private Flyway flyway;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("authorization.jwt.secret", IdentityServiceApplicationTests::randomSecret);
    }

    private static String randomSecret() {
        var bytes = new byte[SECRET_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    void appliesBaselineMigration() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo(BASELINE_VERSION);
    }
}
