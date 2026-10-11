package com.geopslabs.geops.catalog.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Identity's side of US39: the scenarios write BusinessVerified to identity.events with the envelope of E2-02, as
 * Identity's relay does, and wait until Catalog's consumer group has committed it.
 */
public class IdentityEventSteps {
    static final String TOPIC = "identity.events";
    private static final String GROUP = "catalog-service";
    private static final Duration WAIT = Duration.ofSeconds(90);
    private static final long POLL_MILLIS = 100;
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, String> kafka;
    private final KafkaAdmin kafkaAdmin;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;
    private final NearbySearchSteps search;
    private final MutableClock clock;
    private int eventsSent;

    public IdentityEventSteps(KafkaTemplate<String, String> kafka, KafkaAdmin kafkaAdmin, ObjectMapper objectMapper,
                              JdbcTemplate jdbc, NearbySearchSteps search, MutableClock clock) {
        this.kafka = kafka;
        this.kafkaAdmin = kafkaAdmin;
        this.objectMapper = objectMapper;
        this.jdbc = jdbc;
        this.search = search;
        this.clock = clock;
    }

    @Given("Identity published BusinessVerified for business {long} with verified {word}")
    @When("Identity publishes BusinessVerified for business {long} with verified {word}")
    public void identityPublishes(Long businessId, String verified) throws Exception {
        send(businessVerified(UUID.randomUUID(), businessId, Boolean.parseBoolean(verified)));
        awaitConsumed();
    }

    @When("Identity publishes the same BusinessVerified event for business {long} twice")
    public void identityPublishesTwice(Long businessId) throws Exception {
        var event = businessVerified(UUID.randomUUID(), businessId, true);
        send(event);
        send(event);
        awaitConsumed();
    }

    @When("Identity publishes BusinessVerified for business {long} while the database is down for {int} seconds")
    public void identityPublishesWhileTheDatabaseIsDown(Long businessId, int seconds) throws Exception {
        RedisControl.pauseDatabase();
        try {
            send(businessVerified(UUID.randomUUID(), businessId, true));
            Thread.sleep(Duration.ofSeconds(seconds).toMillis());
        } finally {
            RedisControl.resumeDatabase();
        }
        awaitConsumed();
    }

    @When("Identity publishes an event Catalog cannot read and then BusinessVerified for business {long}")
    public void identityPublishesABadEvent(Long businessId) throws Exception {
        sendRaw(String.valueOf(businessId), "{\"eventId\": \"" + UUID.randomUUID()
                + "\", \"eventType\": \"BusinessVerified\", \"payload\": {}}");
        send(businessVerified(UUID.randomUUID(), businessId, true));
        awaitConsumed();
    }

    @Then("{string} has verifiedSeal {word} and business name {string}")
    public void hasSealAndName(String title, String seal, String businessName) throws JsonProcessingException {
        var offer = search.resultTitled(title);
        assertThat(offer.path("verifiedSeal").asBoolean()).isEqualTo(Boolean.parseBoolean(seal));
        assertThat(offer.path("businessName").asText()).isEqualTo(businessName);
    }

    @Then("{string} has verifiedSeal {word}")
    public void hasSeal(String title, String seal) throws JsonProcessingException {
        assertThat(search.resultTitled(title).path("verifiedSeal").asBoolean()).isEqualTo(Boolean.parseBoolean(seal));
    }

    @Then("business {long} has {word} seal change(s) recorded")
    public void sealChangesRecorded(Long businessId, String count) {
        var expected = switch (count) {
            case "no" -> 0;
            case "one" -> 1;
            case "two" -> 2;
            default -> Integer.parseInt(count);
        };
        assertThat(jdbc.queryForObject("SELECT count(*) FROM standing_changes WHERE business_id = ?", Integer.class,
                businessId)).isEqualTo(expected);
    }

    @Then("business {long} has the verified seal")
    public void hasTheVerifiedSeal(Long businessId) {
        assertThat(jdbc.queryForObject("SELECT verified_seal FROM merchant_standings WHERE business_id = ?",
                Boolean.class, businessId)).isTrue();
    }

    private String businessVerified(UUID eventId, Long businessId, boolean verified) throws JsonProcessingException {
        // Each event happens a second after the previous one, later than any copy the scenario saved.
        var occurredAt = clock.instant().plusSeconds(++eventsSent);
        var payload = new LinkedHashMap<String, Object>();
        payload.put("businessId", businessId);
        payload.put("businessName", "Restaurante Don Pepe");
        payload.put("verified", verified);
        payload.put("result", verified ? "APPROVED" : "REJECTED");
        payload.put("reason", verified ? null : "RUC_INACTIVE");
        payload.put("verifiedAt", occurredAt.toString());
        var envelope = new LinkedHashMap<String, Object>();
        envelope.put("eventId", eventId.toString());
        envelope.put("eventType", "BusinessVerified");
        envelope.put("version", 1);
        envelope.put("occurredAt", occurredAt.toString());
        envelope.put("aggregateId", businessId);
        envelope.put("payload", payload);
        return String.valueOf(businessId) + "|" + objectMapper.writeValueAsString(envelope);
    }

    private void send(String keyAndValue) throws Exception {
        var separator = keyAndValue.indexOf('|');
        sendRaw(keyAndValue.substring(0, separator), keyAndValue.substring(separator + 1));
    }

    private void sendRaw(String key, String value) throws InterruptedException, ExecutionException, TimeoutException {
        kafka.send(TOPIC, key, value).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /** Waits until the consumer group committed every record written to the topic. */
    private void awaitConsumed() throws Exception {
        try (var admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            var partitions = admin.describeTopics(java.util.List.of(TOPIC)).allTopicNames().get().get(TOPIC)
                    .partitions().stream().map(info -> new TopicPartition(TOPIC, info.partition())).toList();
            var deadline = Instant.now().plus(WAIT);
            while (Instant.now().isBefore(deadline)) {
                var ends = admin.listOffsets(partitions.stream()
                                .collect(Collectors.toMap(Function.identity(), partition -> OffsetSpec.latest())))
                        .all().get();
                Map<TopicPartition, Long> committed = admin.listConsumerGroupOffsets(GROUP)
                        .partitionsToOffsetAndMetadata().get().entrySet().stream()
                        .filter(entry -> entry.getValue() != null)
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().offset()));
                var done = partitions.stream().allMatch(partition ->
                        committed.getOrDefault(partition, 0L) >= ends.get(partition).offset());
                if (done) {
                    return;
                }
                Thread.sleep(POLL_MILLIS);
            }
        }
        throw new AssertionError("Catalog did not consume identity.events within " + WAIT);
    }
}
