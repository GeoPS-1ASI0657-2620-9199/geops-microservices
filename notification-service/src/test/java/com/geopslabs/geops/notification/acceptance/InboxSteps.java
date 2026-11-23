package com.geopslabs.geops.notification.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.java.en.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Timestamp;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

public class InboxSteps {
    private static final String INSERT = """
            INSERT INTO notifications (recipient_id, notification_type, channel, title, message, related_entity_id, delivery_status, sent_at)
            VALUES (?, ?, 'WEB_PUSH', ?, 'Mensaje de prueba', ?, ?, ?)""";
    private final HttpScenario http;
    private final JdbcTemplate jdbc;
    public InboxSteps(HttpScenario http, JdbcTemplate jdbc) { this.http = http; this.jdbc = jdbc; }

    @Given("consumer {long} has a {word} {word} notice {string} for offer {string} sent at {string}")
    public void notice(Long id, String status, String type, String title, String offer, String sentAt) {
        jdbc.update(INSERT, id, type, title, offer, status, Timestamp.from(Instant.parse(sentAt)));
    }
    @When("consumer {long} lists the notices")
    public void list(Long id) { http.get("/api/v1/notifications", TestIdentity.consumerToken(id)); }
    @When("consumer {long} reads the unread count")
    public void unread(Long id) { http.get("/api/v1/notifications/unread-count", TestIdentity.consumerToken(id)); }
    @When("consumer {long} marks the notices as read")
    public void markRead(Long id) { http.post("/api/v1/notifications/read", TestIdentity.consumerToken(id), "{}"); }
    @When("consumer {long} lists the notices with size {int}")
    public void listSize(Long id, int size) { http.get("/api/v1/notifications?size=" + size, TestIdentity.consumerToken(id)); }
    @When("the notice list is requested without a token")
    public void withoutToken() { http.get("/api/v1/notifications", null); }
    @When("a business owner lists the notices")
    public void ownerList() { http.get("/api/v1/notifications", TestIdentity.businessOwnerToken(9001L, 1L)); }

    @Then("the notice list has {int} items")
    public void items(int count) throws JsonProcessingException { assertThat(http.json(http.last()).size()).isEqualTo(count); }
    @And("notice {int} of the list is {string}")
    public void titleAt(int position, String title) throws JsonProcessingException { assertThat(http.json(http.last()).path(position - 1).path("title").asText()).isEqualTo(title); }
    @And("notice {int} of the list has offerId {string}")
    public void offerAt(int position, String offerId) throws JsonProcessingException { assertThat(http.json(http.last()).path(position - 1).path("offerId").asText()).isEqualTo(offerId); }
    @And("notice {int} of the list is read {word}")
    public void readAt(int position, String read) throws JsonProcessingException { assertThat(http.json(http.last()).path(position - 1).path("read").asText()).isEqualTo(read); }
    @Then("the unread count is {int}")
    public void unreadIs(int count) throws JsonProcessingException { assertThat(http.json(http.last()).path("unread").asInt()).isEqualTo(count); }
}
