package com.geopslabs.geops.reservation.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.reservation.domain.events.ReservationRedeemed;
import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxEventJpaEntity;
import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxEventJpaRepository;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Kafka runs in Testcontainers for the whole suite. "Kafka is stopped" pauses the container, so the broker
 * stops answering exactly as a crashed one would, and the after-hook resumes it for the next scenario.
 */
public class OutboxSteps {
    private static final Duration PUBLISH_WAIT = Duration.ofSeconds(30);
    private static final Duration POLL = Duration.ofMillis(500);
    private static final long WAIT_STEP_MS = 200;

    private final OutboxEventJpaRepository outbox;
    private final ObjectMapper objectMapper;
    private final ScenarioState state;
    private boolean kafkaPaused;

    public OutboxSteps(OutboxEventJpaRepository outbox, ObjectMapper objectMapper, ScenarioState state) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.state = state;
    }

    @After
    public void bringKafkaBack() {
        if (kafkaPaused) {
            kafkaIsBack();
        }
    }

    @Given("Kafka is stopped")
    public void kafkaIsStopped() {
        var kafka = CucumberSpringConfiguration.KAFKA;
        kafka.getDockerClient().pauseContainerCmd(kafka.getContainerId()).exec();
        kafkaPaused = true;
    }

    @When("Kafka is back")
    public void kafkaIsBack() {
        var kafka = CucumberSpringConfiguration.KAFKA;
        kafka.getDockerClient().unpauseContainerCmd(kafka.getContainerId()).exec();
        kafkaPaused = false;
    }

    @And("after {int} seconds the outbox row of the event is still unpublished")
    public void rowIsStillUnpublished(int seconds) throws InterruptedException {
        Thread.sleep(Duration.ofSeconds(seconds).toMillis());
        assertThat(eventRow().getPublishedAt()).isNull();
    }

    @Then("the outbox row of the event is marked as published")
    public void rowIsMarkedAsPublished() throws InterruptedException {
        var deadline = Instant.now().plus(PUBLISH_WAIT);
        while (Instant.now().isBefore(deadline)) {
            if (eventRow().getPublishedAt() != null) {
                return;
            }
            Thread.sleep(WAIT_STEP_MS);
        }
        fail("The outbox row of reservation %d was not published", state.reservationId());
    }

    @And("the ReservationRedeemed event of the reservation arrives at topic {string} with its eventId")
    public void eventArrivesAtTopic(String topic) throws IOException {
        var eventId = eventRow().getId();
        var record = findRecord(topic, eventId)
                .orElseThrow(() -> new AssertionError("Event " + eventId + " did not arrive at " + topic));
        var envelope = objectMapper.readTree(record.value());
        assertThat(record.key()).isEqualTo(String.valueOf(state.reservationId()));
        assertThat(envelope.path("eventType").asText()).isEqualTo(ReservationRedeemed.EVENT_TYPE);
        assertThat(envelope.path("version").asInt()).isEqualTo(ReservationRedeemed.VERSION);
        assertThat(envelope.path("aggregateId").asLong()).isEqualTo(state.reservationId());
        assertThat(envelope.path("payload").path("reservationId").asLong()).isEqualTo(state.reservationId());
        assertThat(envelope.path("payload").path("redeemedAt").asText()).isNotBlank();
    }

    private OutboxEventJpaEntity eventRow() {
        var rows = outbox.findByAggregateIdAndEventType(state.reservationId(), ReservationRedeemed.EVENT_TYPE);
        assertThat(rows).hasSize(1);
        return rows.get(0);
    }

    private Optional<ConsumerRecord<String, String>> findRecord(String topic, UUID eventId) throws IOException {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, CucumberSpringConfiguration.KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "acceptance-" + UUID.randomUUID(),
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        try (var consumer = new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer())) {
            var partitions = consumer.partitionsFor(topic).stream()
                    .map(info -> new TopicPartition(topic, info.partition()))
                    .toList();
            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);
            var deadline = Instant.now().plus(PUBLISH_WAIT);
            while (Instant.now().isBefore(deadline)) {
                for (var record : consumer.poll(POLL)) {
                    if (eventId.toString().equals(objectMapper.readTree(record.value()).path("eventId").asText())) {
                        return Optional.of(record);
                    }
                }
            }
        }
        return Optional.empty();
    }
}
