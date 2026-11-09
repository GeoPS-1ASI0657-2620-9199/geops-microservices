package com.geopslabs.geops.reservation.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.geopslabs.geops.reservation.application.services.DeadlineScheduler;
import com.geopslabs.geops.reservation.domain.models.ReportStatus;
import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import com.geopslabs.geops.reservation.infrastructure.persistence.NonComplianceReportJpaEntity;
import com.geopslabs.geops.reservation.infrastructure.persistence.NonComplianceReportJpaRepository;
import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxEventJpaRepository;
import com.geopslabs.geops.reservation.infrastructure.persistence.ReportAnswerJpaRepository;
import com.geopslabs.geops.reservation.infrastructure.persistence.ReservationJpaEntity;
import com.geopslabs.geops.reservation.infrastructure.persistence.ReservationJpaRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US42 and US43. The ids of the scenarios (reservation 5013) are labels: the database assigns the real ids, and the
 * steps translate between the two so the scenarios read like the examples of the BRD.
 */
public class ReportSteps {
    private static final String RESERVATION_REPORTS = "/api/v1/reservations/%d/reports";
    private static final String REPORTS = "/api/v1/reports";
    private static final String REASON_BODY = "{\"reason\": \"%s\"}";
    private static final String TEXT_BODY = "{\"text\": \"%s\"}";
    private static final String CONSUMER_FAVOR_BODY = "{\"status\": \"CLOSED\", \"resolution\": \"CONSUMER_FAVOR\"}";
    private static final String OFFER_TITLE = "2x1 en almuerzos ejecutivos";
    private static final String VALID_REASON = "No respetaron el precio publicado.";
    private static final Long BUSINESS_OWNER_USER_ID = 9001L;
    private static final Duration RESPONSE_WINDOW = Duration.ofHours(48);

    private final HttpScenario http;
    private final MutableClock clock;
    private final ReservationJpaRepository reservations;
    private final NonComplianceReportJpaRepository reports;
    private final ReportAnswerJpaRepository answers;
    private final OutboxEventJpaRepository outbox;
    private final DeadlineScheduler scheduler;
    private final Map<Long, Long> ids = new HashMap<>();
    private long nextOffer = 1052;

    public ReportSteps(HttpScenario http, MutableClock clock, ReservationJpaRepository reservations,
                       NonComplianceReportJpaRepository reports, ReportAnswerJpaRepository answers,
                       OutboxEventJpaRepository outbox, DeadlineScheduler scheduler) {
        this.http = http;
        this.clock = clock;
        this.reservations = reservations;
        this.reports = reports;
        this.answers = answers;
        this.outbox = outbox;
        this.scheduler = scheduler;
    }

    /** Reports reference reservations, so they go first; the hook of ReservationSteps then clears the reservations. */
    @Before(order = 0)
    public void clearReports() {
        answers.deleteAll();
        reports.deleteAll();
    }

    @Given("consumer {long} has an ACTIVE reservation {long} with code {string} of business {long} expiring at {string}")
    public void consumerHasActiveReservation(Long consumerId, Long label, String code, Long businessId,
                                             String expiresAt) {
        ids.put(label, store(consumerId, code, businessId, Instant.parse(expiresAt), ReservationStatus.ACTIVE));
    }

    @Given("reservation {long} is {}")
    public void reservationIs(Long label, String state) {
        var consumerId = 2001L;
        var expiresAt = Instant.parse("2026-10-29T04:59:59Z");
        var status = ReservationStatus.ACTIVE;
        switch (state) {
            case "REDEEMED" -> status = ReservationStatus.REDEEMED;
            case "ACTIVE but past its deadline" -> expiresAt = Instant.parse("2026-10-28T04:59:59Z");
            case "of consumer 2002" -> consumerId = 2002L;
            case "already reported" -> status = ReservationStatus.REPORTED;
            default -> throw new IllegalArgumentException(state);
        }
        var real = store(consumerId, "Q4ZT8K2N", 84L, expiresAt, status);
        ids.put(label, real);
        if (status == ReservationStatus.REPORTED) {
            storeReport(real, consumerId, 84L, clock.instant());
        }
    }

    @Given("consumer {long} reported reservation {long}")
    public void consumerReported(Long consumerId, Long label) {
        consumerReports(consumerId, label, VALID_REASON);
        assertThat(http.last().status()).as(http.last().body()).isEqualTo(201);
    }

    @Given("consumer {long} reported reservation {long} of business {long} at {string} with reason {string}")
    public void consumerReportedAt(Long consumerId, Long label, Long businessId, String openedAt, String reason) {
        var real = store(consumerId, "Q4ZT8K2M", businessId, Instant.parse("2026-10-29T04:59:59Z"),
                ReservationStatus.REPORTED);
        ids.put(label, real);
        var entity = storeReport(real, consumerId, businessId, Instant.parse(openedAt));
        entity.setReason(reason);
        reports.save(entity);
    }

    @When("consumer {long} reports reservation {long} with reason {string}")
    public void consumerReports(Long consumerId, Long label, String reason) {
        http.post(RESERVATION_REPORTS.formatted(ids.get(label)), TestIdentity.consumerToken(consumerId),
                REASON_BODY.formatted(reason));
    }

    @When("consumer {long} opens reservation {long}")
    public void consumerOpens(Long consumerId, Long label) {
        http.get("/api/v1/reservations/" + ids.get(label), TestIdentity.consumerToken(consumerId));
    }

    @When("the deadline scheduler runs")
    public void schedulerRuns() {
        scheduler.runOnce();
    }

    @When("the business owner of business {long} lists the reports")
    public void businessLists(Long businessId) {
        http.get(REPORTS, ownerToken(businessId));
    }

    @When("the business owner of business {long} lists the {word} reports")
    public void businessListsByStatus(Long businessId, String status) {
        http.get(REPORTS + "?status=" + status, ownerToken(businessId));
    }

    @When("consumer {long} lists their reports")
    public void consumerLists(Long consumerId) {
        http.get(REPORTS, TestIdentity.consumerToken(consumerId));
    }

    @When("the business owner of business {long} answers the report with {string}")
    public void businessAnswers(Long businessId, String text) {
        http.post(REPORTS + "/" + onlyReportId() + "/responses", ownerToken(businessId), TEXT_BODY.formatted(text));
    }

    @Given("the business owner of business {long} answered the report with {string}")
    public void businessAnswered(Long businessId, String text) {
        businessAnswers(businessId, text);
        assertThat(http.last().status()).as(http.last().body()).isEqualTo(201);
    }

    @When("the business owner of business {long} resolves the report in favor of the consumer")
    public void businessResolves(Long businessId) {
        http.patch(REPORTS + "/" + onlyReportId() + "/status", ownerToken(businessId), CONSUMER_FAVOR_BODY);
    }

    @When("the business owner of business {long} resolves the report in favor of the business")
    public void businessResolvesInItsFavor(Long businessId) {
        http.patch(REPORTS + "/" + onlyReportId() + "/status", ownerToken(businessId),
                "{\"status\": \"CLOSED\", \"resolution\": \"BUSINESS_FAVOR\"}");
    }

    @When("consumer {long} opens the report")
    public void consumerOpensTheReport(Long consumerId) {
        http.get(REPORTS + "/" + onlyReportId(), TestIdentity.consumerToken(consumerId));
    }

    @When("a request without token lists the reports")
    public void anonymousLists() {
        http.get(REPORTS, null);
    }

    @Then("the report has status {string}, openedAt {string} and responseDeadline {string}")
    public void reportHasDates(String status, String openedAt, String deadline) throws JsonProcessingException {
        var body = http.json(http.last());
        assertThat(body.path("status").asText()).isEqualTo(status);
        assertThat(body.path("openedAt").asText()).isEqualTo(openedAt);
        assertThat(body.path("responseDeadline").asText()).isEqualTo(deadline);
        assertThat(body.path("reservationCode").asText()).isEqualTo("Q4ZT8K2M");
    }

    @Then("the report has status {string} and resolution {string}")
    public void reportHasResolution(String status, String resolution) {
        var report = reports.findAll().get(0);
        assertThat(report.getStatus().name()).isEqualTo(status);
        assertThat(report.getResolution().name()).isEqualTo(resolution);
    }

    @And("the response has status {string} and resolution {string}")
    public void responseHasResolution(String status, String resolution) throws JsonProcessingException {
        var body = http.json(http.last());
        assertThat(body.path("status").asText()).isEqualTo(status);
        assertThat(body.path("resolution").asText()).isEqualTo(resolution);
    }

    @And("the report is stored as {string}")
    public void reportIsStoredAs(String status) {
        assertThat(reports.findAll()).singleElement()
                .satisfies(report -> assertThat(report.getStatus()).isEqualTo(ReportStatus.valueOf(status)));
    }

    @And("a {word} event for reservation {long} is in the outbox")
    public void eventInOutbox(String eventType, Long label) {
        assertThat(outbox.findByAggregateIdAndEventType(ids.get(label), eventType)).hasSize(1);
    }

    @And("a ReportResolved event with closedBy {string} and previousStatus {string} is in the outbox")
    public void resolvedEvent(String closedBy, String previousStatus) {
        var rows = outbox.findAll().stream().filter(row -> "ReportResolved".equals(row.getEventType())).toList();
        assertThat(rows).hasSize(1);
        var compact = rows.get(0).getPayload().replace(" ", "");
        assertThat(compact).contains("\"closedBy\":\"" + closedBy + "\"")
                .contains("\"previousStatus\":\"" + previousStatus + "\"");
    }

    @And("no report event is in the outbox")
    public void noReportEvent() {
        assertThat(outbox.findAll()).noneMatch(row -> row.getEventType().startsWith("Report"));
    }

    @Then("the reservation has status {string}")
    public void reservationHasStatus(String status) throws JsonProcessingException {
        assertThat(http.json(http.last()).path("status").asText()).isEqualTo(status);
    }

    @And("its report has responseDeadline {string}")
    public void itsReportHasDeadline(String deadline) throws JsonProcessingException {
        assertThat(http.json(http.last()).path("report").path("responseDeadline").asText()).isEqualTo(deadline);
    }

    @Then("reservation {long} has status {string}")
    public void storedReservationHasStatus(Long label, String status) {
        assertThat(reservations.findById(ids.get(label)).orElseThrow().getStatus().name()).isEqualTo(status);
    }

    @Then("the list has {int} report(s) with status {string}")
    public void listHas(int count, String status) throws JsonProcessingException {
        var body = http.json(http.last());
        assertThat(body.size()).isEqualTo(count);
        if (count > 0) {
            assertThat(body.get(0).path("status").asText()).isEqualTo(status);
        }
    }

    @And("the report has reason {string}, reservation code {string} and responseDeadline {string}")
    public void reportHasDetail(String reason, String code, String deadline) throws JsonProcessingException {
        var first = http.json(http.last()).get(0);
        assertThat(first.path("reason").asText()).isEqualTo(reason);
        assertThat(first.path("reservationCode").asText()).isEqualTo(code);
        assertThat(first.path("offerTitle").asText()).isEqualTo(OFFER_TITLE);
        assertThat(first.path("responseDeadline").asText()).isEqualTo(deadline);
    }

    @And("the response shows the answer {string}")
    public void responseShowsAnswer(String text) throws JsonProcessingException {
        assertThat(http.json(http.last()).path("response").path("text").asText()).isEqualTo(text);
    }

    private Long store(Long consumerId, String code, Long businessId, Instant expiresAt, ReservationStatus status) {
        var entity = new ReservationJpaEntity();
        entity.setCode(code);
        entity.setConsumerId(consumerId);
        entity.setOfferId(nextOffer++);
        entity.setBusinessId(businessId);
        entity.setOfferTitle(OFFER_TITLE);
        entity.setReservedAt(Instant.parse("2026-10-27T15:00:00Z"));
        entity.setExpiresAt(expiresAt);
        entity.setStatus(status);
        return reservations.save(entity).getId();
    }

    private NonComplianceReportJpaEntity storeReport(Long reservationId, Long consumerId, Long businessId,
                                                     Instant openedAt) {
        var entity = new NonComplianceReportJpaEntity();
        entity.setReservationId(reservationId);
        entity.setConsumerId(consumerId);
        entity.setBusinessId(businessId);
        entity.setReason("No me aplicaron el 2x1.");
        entity.setOpenedAt(openedAt);
        entity.setResponseDeadline(openedAt.plus(RESPONSE_WINDOW));
        entity.setStatus(ReportStatus.OPEN);
        return reports.save(entity);
    }

    private Long onlyReportId() {
        return reports.findAll().get(0).getId();
    }

    private static String ownerToken(Long businessId) {
        return TestIdentity.businessOwnerToken(BUSINESS_OWNER_USER_ID, businessId);
    }
}
