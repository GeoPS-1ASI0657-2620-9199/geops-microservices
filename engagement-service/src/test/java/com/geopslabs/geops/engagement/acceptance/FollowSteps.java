package com.geopslabs.geops.engagement.acceptance;

import com.geopslabs.geops.engagement.domain.ports.FollowRepositoryPort;
import com.geopslabs.geops.engagement.infrastructure.persistence.OutboxEventJpaRepository;
import io.cucumber.java.en.*;
import static org.assertj.core.api.Assertions.*;

public class FollowSteps {
    private final HttpScenario http;
    private final FollowRepositoryPort follows;
    private final OutboxEventJpaRepository outbox;

    public FollowSteps(HttpScenario http, FollowRepositoryPort follows, OutboxEventJpaRepository outbox) {
        this.http = http;
        this.follows = follows;
        this.outbox = outbox;
    }

    @When("consumer {long} follows business {long}")
    public void follow(Long consumerId, Long businessId) {
        http.post("/api/v1/follows", TestIdentity.consumerToken(consumerId), "{\"businessId\":" + businessId + "}");
    }

    @When("consumer {long} unfollows business {long}")
    public void unfollow(Long consumerId, Long businessId) {
        http.delete("/api/v1/follows/" + businessId, TestIdentity.consumerToken(consumerId));
    }

    @When("consumer {long} lists followed businesses")
    public void list(Long consumerId) {
        http.get("/api/v1/follows", TestIdentity.consumerToken(consumerId));
    }

    @Then("consumer {long} follows no business")
    public void empty(Long consumerId) {
        assertThat(follows.findByConsumer(consumerId)).isEmpty();
    }

    @Then("the followed business has {int} active offers")
    public void activeOffers(int count) throws Exception {
        assertThat(http.json(http.last()).get(0).get("activeOffersCount").asInt()).isEqualTo(count);
    }

    @Then("there is {long} {word} event for consumer {long}")
    public void event(long count, String type, Long consumerId) {
        assertThat(outbox.findAll().stream().filter(e -> e.getAggregateId().equals(consumerId) && e.getEventType().equals(type)).count()).isEqualTo(count);
    }
}
