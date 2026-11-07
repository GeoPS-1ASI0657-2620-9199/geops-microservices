package com.geopslabs.geops.catalog.acceptance;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.infrastructure.messaging.EventContract;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reservation's side of US44: the scenarios write the examples of BRD-11 to reservation.events, with a new eventId
 * and the business of the scenario, as Reservation's relay does, and wait until Catalog's consumer group has
 * committed them.
 */
public class ReservationEventSteps {
    static final String TOPIC = "reservation.events";
    private static final String RESERVATION_REDEEMED = "ReservationRedeemed";
    private static final String REPORT_OPENED = "ReportOpened";
    private static final String REPORT_ANSWERED = "ReportAnswered";
    private static final String REPORT_RESOLVED = "ReportResolved";
    private static final long SEND_TIMEOUT_SECONDS = 10;
    private static final Duration VERIFIED_BEFORE = Duration.ofDays(1);
    private static final int NO_OPEN_REPORTS = 0;

    private final KafkaTemplate<String, String> kafka;
    private final KafkaAdmin kafkaAdmin;
    private final JdbcTemplate jdbc;
    private final MerchantStandingRepositoryPort standings;
    private final MutableClock clock;

    public ReservationEventSteps(KafkaTemplate<String, String> kafka, KafkaAdmin kafkaAdmin, JdbcTemplate jdbc,
                                 MerchantStandingRepositoryPort standings, MutableClock clock) {
        this.kafka = kafka;
        this.kafkaAdmin = kafkaAdmin;
        this.jdbc = jdbc;
        this.standings = standings;
        this.clock = clock;
    }

    @And("businesses {long} and {long} have a verified RUC")
    public void businessesHaveAVerifiedRuc(Long first, Long second) {
        verifyRuc(first);
        verifyRuc(second);
    }

    @Given("business {long} lost the seal with {int} redemptions and {int} report(s) in favor of the consumer")
    public void businessLostTheSeal(Long businessId, int redeemed, int consumerFavor) {
        var current = standings.findByBusinessId(businessId).orElseThrow();
        var index = MerchantStanding.complianceIndexOf(redeemed, consumerFavor);
        standings.save(new MerchantStanding(businessId, current.getBusinessName(), true, NO_OPEN_REPORTS, index,
                MerchantStanding.sealFor(true, index), current.getUpdatedAt(), redeemed, consumerFavor));
    }

    @Given("Reservation published {int} ReservationRedeemed events for business {long}")
    @When("Reservation publishes {int} ReservationRedeemed event(s) for business {long}")
    public void reservationPublishesRedemptions(int count, Long businessId) throws Exception {
        for (var sent = 0; sent < count; sent++) {
            send(event(RESERVATION_REDEEMED, businessId));
        }
        ConsumerGroupProgress.awaitConsumed(kafkaAdmin, TOPIC);
    }

    @Given("Reservation published ReportOpened for business {long}")
    @When("Reservation publishes ReportOpened for business {long}")
    public void reservationPublishesReportOpened(Long businessId) throws Exception {
        publish(REPORT_OPENED, businessId);
    }

    @When("Reservation publishes ReportAnswered for business {long}")
    public void reservationPublishesReportAnswered(Long businessId) throws Exception {
        publish(REPORT_ANSWERED, businessId);
    }

    @When("Reservation publishes ReportOpened and then ReportResolved in favor of the consumer for business {long}")
    public void reservationOpensAndResolvesInFavorOfTheConsumer(Long businessId) throws Exception {
        send(event(REPORT_OPENED, businessId));
        send(event(REPORT_RESOLVED, businessId));
        ConsumerGroupProgress.awaitConsumed(kafkaAdmin, TOPIC);
    }

    @When("Reservation publishes the same ReportOpened event for business {long} twice")
    public void reservationPublishesTheSameEventTwice(Long businessId) throws Exception {
        var opened = event(REPORT_OPENED, businessId);
        send(opened);
        send(opened);
        ConsumerGroupProgress.awaitConsumed(kafkaAdmin, TOPIC);
    }

    @Then("business {long} has compliance index {bigdecimal} and no verified seal")
    public void hasIndexWithoutSeal(Long businessId, BigDecimal index) {
        assertStanding(businessId, index, false);
    }

    @Then("business {long} has compliance index {bigdecimal} and a verified seal")
    public void hasIndexWithSeal(Long businessId, BigDecimal index) {
        assertStanding(businessId, index, true);
    }

    @And("business {long} has a seal change with reason {string}")
    public void hasSealChangeWithReason(Long businessId, String reason) {
        assertThat(jdbc.queryForList("SELECT reason FROM standing_changes WHERE business_id = ?", String.class,
                businessId)).contains(reason);
    }

    @Then("business {long} has {int} open report(s)")
    public void hasOpenReports(Long businessId, int openReports) {
        assertThat(standings.findByBusinessId(businessId).orElseThrow().getOpenReports()).isEqualTo(openReports);
    }

    private void verifyRuc(Long businessId) {
        var current = standings.findByBusinessId(businessId).orElseThrow();
        standings.save(current.applyRucVerification(true, null, clock.instant().minus(VERIFIED_BEFORE)));
    }

    private void assertStanding(Long businessId, BigDecimal index, boolean seal) {
        var standing = standings.findByBusinessId(businessId).orElseThrow();
        assertThat(standing.getComplianceIndex()).isEqualByComparingTo(index);
        assertThat(standing.hasVerifiedSeal()).isEqualTo(seal);
    }

    private void publish(String eventType, Long businessId) throws Exception {
        send(event(eventType, businessId));
        ConsumerGroupProgress.awaitConsumed(kafkaAdmin, TOPIC);
    }

    private ObjectNode event(String eventType, Long businessId) {
        var envelope = (ObjectNode) EventContract.example(eventType);
        envelope.put("eventId", UUID.randomUUID().toString());
        envelope.put("occurredAt", clock.instant().toString());
        ((ObjectNode) envelope.path("payload")).put("businessId", businessId);
        return envelope;
    }

    private void send(ObjectNode envelope) throws Exception {
        kafka.send(TOPIC, envelope.path("aggregateId").asText(), envelope.toString())
                .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
