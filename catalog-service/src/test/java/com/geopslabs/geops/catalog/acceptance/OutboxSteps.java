package com.geopslabs.geops.catalog.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.catalog.domain.events.OfferPublished;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.infrastructure.persistence.OutboxEventJpaEntity;
import com.geopslabs.geops.catalog.infrastructure.persistence.OutboxEventJpaRepository;
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
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OutboxSteps {
    static final String TOPIC = "catalog.events";
    private static final Duration PUBLISH_WAIT = Duration.ofSeconds(30);
    private static final Duration POLL = Duration.ofMillis(500);
    private static final String DEFAULT_BUSINESS_NAME = "Restaurante Don Pepe";
    private static final String ADDRESS = "Av. Larco 345, Miraflores";
    private static final String CONDITIONS = "Lunes a viernes de 12:00 a 15:00";
    private static final String CATEGORY = "Gastronomía";
    private static final BigDecimal PRICE = new BigDecimal("18.50");
    private static final double STORE_LATITUDE = -12.1211;
    private static final double STORE_LONGITUDE = -77.0297;
    private static final int RADIUS_METERS = 1_200;
    private static final int CAMPAIGN_DAYS = 19;
    private static final long OWNER_USER_ID = 9001L;
    private static final String GROUP_PREFIX = "acceptance-";

    private final HttpScenario http;
    private final ObjectMapper objectMapper;
    private final OutboxEventJpaRepository outbox;
    private final MerchantStandingRepositoryPort standings;
    private final MutableClock clock;
    private final List<String> businessNames = new ArrayList<>();
    private boolean kafkaPaused;
    private Long campaignId;

    public OutboxSteps(HttpScenario http, ObjectMapper objectMapper, OutboxEventJpaRepository outbox,
                       MerchantStandingRepositoryPort standings, MutableClock clock) {
        this.http = http;
        this.objectMapper = objectMapper;
        this.outbox = outbox;
        this.standings = standings;
        this.clock = clock;
    }

    @After
    public void bringKafkaBack() {
        if (kafkaPaused) {
            kafkaIsBack();
        }
    }

    @Given("business {long} {string} is known to Catalog")
    public void businessIsKnown(Long businessId, String name) {
        standings.save(MerchantStanding.provisional(businessId, name));
        businessNames.add(name);
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

    @When("business owner of business {long} publishes a campaign with offers {string} and {string}")
    public void publishesTwoOffers(Long businessId, String first, String second) throws JsonProcessingException {
        publish(businessId, List.of(first, second));
    }

    @When("business owner of business {long} publishes a campaign with offer {string}")
    public void publishesOneOffer(Long businessId, String title) throws JsonProcessingException {
        publish(businessId, List.of(title));
    }

    @And("after {int} seconds the outbox rows of the campaign are still unpublished")
    public void rowsAreStillUnpublished(int seconds) throws InterruptedException {
        Thread.sleep(Duration.ofSeconds(seconds).toMillis());
        assertThat(rowsOfTheCampaign()).isNotEmpty().allSatisfy(row -> assertThat(row.getPublishedAt()).isNull());
    }

    @Then("{int} OfferPublished event(s) of the campaign arrive(s) at topic {string} with the campaign id as key")
    public void eventsArrive(int expected, String topic) throws IOException {
        var eventIds = rowsOfTheCampaign().stream().map(row -> row.getId().toString()).toList();
        assertThat(eventIds).hasSize(expected);
        var records = findRecords(topic, eventIds);
        assertThat(records).as("events of campaign %d in %s", campaignId, topic).hasSize(expected);
        for (var record : records) {
            var envelope = objectMapper.readTree(record.value());
            assertThat(record.key()).isEqualTo(String.valueOf(campaignId));
            assertThat(envelope.path("eventType").asText()).isEqualTo(OfferPublished.EVENT_TYPE);
            assertThat(envelope.path("aggregateId").asLong()).isEqualTo(campaignId);
            assertThat(envelope.path("payload").path("businessName").asText()).isIn(businessNames);
        }
    }

    private void publish(Long businessId, List<String> titles) throws JsonProcessingException {
        var today = LocalDate.now(clock);
        var store = Map.of("latitude", STORE_LATITUDE, "longitude", STORE_LONGITUDE);
        var offers = titles.stream().map(title -> Map.of("title", title, "conditions", CONDITIONS, "price", PRICE,
                "validTo", today.plusDays(CAMPAIGN_DAYS).toString(), "category", CATEGORY)).toList();
        var body = Map.of(
                "businessName", businessNames.isEmpty() ? DEFAULT_BUSINESS_NAME : businessNames.get(0),
                "name", titles.get(0),
                "description", String.join(", ", titles),
                "period", Map.of("start", today.toString(), "end", today.plusDays(CAMPAIGN_DAYS).toString()),
                "storeLocation", Map.of("address", ADDRESS, "latitude", STORE_LATITUDE,
                        "longitude", STORE_LONGITUDE),
                "zone", Map.of("type", "RADIUS", "center", store, "radiusMeters", RADIUS_METERS),
                "offers", offers);
        var token = TestIdentity.businessOwnerToken(OWNER_USER_ID, businessId);
        var created = http.post("/api/v1/campaigns", token, objectMapper.writeValueAsString(body));
        campaignId = http.json(created).path("campaignId").asLong();
    }

    private List<OutboxEventJpaEntity> rowsOfTheCampaign() {
        return outbox.findByAggregateIdAndEventType(campaignId, OfferPublished.EVENT_TYPE);
    }

    private List<ConsumerRecord<String, String>> findRecords(String topic, List<String> eventIds) throws IOException {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, CucumberSpringConfiguration.KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, GROUP_PREFIX + UUID.randomUUID(),
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        var found = new LinkedHashMap<String, ConsumerRecord<String, String>>();
        try (var consumer = new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer())) {
            var partitions = consumer.partitionsFor(topic).stream()
                    .map(info -> new TopicPartition(topic, info.partition()))
                    .toList();
            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);
            var deadline = Instant.now().plus(PUBLISH_WAIT);
            while (found.size() < eventIds.size() && Instant.now().isBefore(deadline)) {
                for (var record : consumer.poll(POLL)) {
                    keepIfExpected(record, eventIds, found);
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    private void keepIfExpected(ConsumerRecord<String, String> record, List<String> eventIds,
                                Map<String, ConsumerRecord<String, String>> found) throws JsonProcessingException {
        var eventId = eventIdOf(record);
        if (eventIds.contains(eventId)) {
            found.putIfAbsent(eventId, record);
        }
    }

    private String eventIdOf(ConsumerRecord<String, String> record) throws JsonProcessingException {
        JsonNode envelope = objectMapper.readTree(record.value());
        return envelope.path("eventId").asText();
    }
}
