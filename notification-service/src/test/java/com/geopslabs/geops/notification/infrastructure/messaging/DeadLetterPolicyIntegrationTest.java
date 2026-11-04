package com.geopslabs.geops.notification.infrastructure.messaging;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * The policy of MessagingConfiguration against a real broker. A listener that exists only in this test fails in
 * the three ways an event can fail, and the test checks where each one ends and how many times it was tried.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"geops.outbox.relay-enabled=false", "geops.messaging.enabled=false"})
@Import(DeadLetterPolicyIntegrationTest.FailingListener.class)
class DeadLetterPolicyIntegrationTest {
    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres");
    private static final String TOPIC = "test.events";
    private static final String DLT = TOPIC + ".DLT";
    private static final Map<String, AtomicInteger> ATTEMPTS = new ConcurrentHashMap<>();

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE)
            .withInitScript("acceptance/enable-postgis.sql");
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.7.0");

    static {
        POSTGRES.start();
        KAFKA.start();
        try (var admin = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic(TOPIC, 1, (short) 1), new NewTopic(DLT, 1, (short) 1))).all().get();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @DynamicPropertySource
    static void kafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, String> template;

    @Test
    void anUnreadableEventGoesToTheDeadLetterTopicAtOnceAndTheNextOneIsStillProcessed() {
        template.send(TOPIC, "unreadable", "no-json");
        template.send(TOPIC, "good", "ok");

        var dead = deadLettered("unreadable");

        assertThat(dead.headers().lastHeader("kafka_dlt-exception-fqcn")).isNotNull();
        assertThat(ATTEMPTS.get("no-json")).hasValue(1);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(ATTEMPTS.get("ok")).hasValue(1));
    }

    @Test
    void aFailureOfTheRuleIsTriedFiveTimesAndThenKeptWithItsCause() {
        template.send(TOPIC, "rule", "bug");

        var dead = deadLettered("rule");

        assertThat(ATTEMPTS.get("bug")).hasValue(5);
        assertThat(new String(dead.headers().lastHeader("kafka_dlt-exception-fqcn").value()))
                .contains("ListenerExecutionFailedException");
    }

    private ConsumerRecord<String, String> deadLettered(String key) {
        var props = Map.<String, Object>of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-reader-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        var found = new ArrayList<ConsumerRecord<String, String>>();
        try (var consumer = new KafkaConsumer<String, String>(props)) {
            consumer.subscribe(List.of(DLT));
            await().atMost(Duration.ofSeconds(60)).until(() -> {
                consumer.poll(Duration.ofMillis(500)).forEach(found::add);
                return found.stream().anyMatch(record -> key.equals(record.key()));
            });
        }
        return found.stream().filter(record -> key.equals(record.key())).findFirst().orElseThrow();
    }

    @Component
    static class FailingListener {
        @KafkaListener(id = "dead-letter-policy-test", topics = TOPIC, groupId = "dead-letter-policy-test")
        void onEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
            ATTEMPTS.computeIfAbsent(record.value(), value -> new AtomicInteger()).incrementAndGet();
            switch (record.value()) {
                case "no-json" -> throw new UnreadableEventException("not an event");
                case "bug" -> throw new IllegalStateException("a rule failed");
                default -> acknowledgment.acknowledge();
            }
        }
    }
}
