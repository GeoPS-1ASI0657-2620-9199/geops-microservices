package com.geopslabs.geops.reservation.acceptance;

import io.cucumber.spring.CucumberContextConfiguration;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AcceptanceTestConfiguration.class)
public class CucumberSpringConfiguration {
    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres");

    /** Same image as platform/docker-compose.yml, with the topic created up front as kafka-init does. */
    static final String KAFKA_IMAGE = "apache/kafka:3.7.0";
    static final String RESERVATION_EVENTS = "reservation.events";
    private static final int PARTITIONS = 3;
    private static final short REPLICATION_FACTOR = 1;
    private static final String RELAY_DELAY_MS = "200";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE);

    static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    static {
        POSTGRES.start();
        KAFKA.start();
        createTopic();
    }

    @DynamicPropertySource
    static void identityJwks(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", TestIdentity::jwksUri);
    }

    @DynamicPropertySource
    static void outboxRelay(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("geops.outbox.relay-enabled", () -> "true");
        registry.add("geops.outbox.relay-delay-ms", () -> RELAY_DELAY_MS);
    }

    private static void createTopic() {
        try (var admin = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic(RESERVATION_EVENTS, PARTITIONS, REPLICATION_FACTOR))).all().get();
        } catch (ExecutionException exception) {
            throw new IllegalStateException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
