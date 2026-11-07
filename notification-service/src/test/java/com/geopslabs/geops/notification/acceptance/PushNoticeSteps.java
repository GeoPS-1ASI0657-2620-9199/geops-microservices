package com.geopslabs.geops.notification.acceptance;

import com.geopslabs.geops.notification.application.usecases.*;
import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.*;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

public class PushNoticeSteps {
    private final RecipientRepositoryPort recipients;
    private final NotificationPreferenceRepositoryPort preferences;
    private final LastKnownLocationRepositoryPort locations;
    private final RegisterPushSubscriptionUseCase subscriptions;
    private final RecipientProjectionPort follows;
    private final PlanOfferNoticesUseCase planner;
    private final RecordingPushChannel push;
    private final MutableClock clock;
    public PushNoticeSteps(RecipientRepositoryPort recipients, NotificationPreferenceRepositoryPort preferences, LastKnownLocationRepositoryPort locations, RegisterPushSubscriptionUseCase subscriptions, RecipientProjectionPort follows, PlanOfferNoticesUseCase planner, RecordingPushChannel push, MutableClock clock) { this.recipients = recipients; this.preferences = preferences; this.locations = locations; this.subscriptions = subscriptions; this.follows = follows; this.planner = planner; this.push = push; this.clock = clock; }
    @Before(order = -5)
    public void reset() { push.clear(); }
    @Given("consumer {long} allowed push with a browser subscription")
    public void consumer(Long id) {
        recipients.upsert(new Recipient(id, "consumer@example.test", true, "CONSUMER"));
        preferences.upsert(new NotificationPreference(id, true, false, 3, LocalDateTime.now(clock)));
        subscriptions.register(id, "test-browser-token", "Test browser");
    }
    @Given("consumer {long} was nearby {int} minutes ago with radius {int} meters")
    public void location(Long id, int minutes, int radius) { locations.saveIfNewer(id, new LastKnownLocation(new GeoPoint(-12.1211, -77.0297), 20, radius, LocalDateTime.now(clock).minusMinutes(minutes))); }
    @Given("consumer {long} follows the publishing business")
    public void follow(Long id) { follows.follow(id, 84L); }
    @When("Catalog publishes the same nearby push offer {int} times")
    public void publish(int count) {
        var offer = new OfferPublishedEvent(1052L, 84L, "Don Pepe", "2x1", LocalDate.parse("2026-11-15"), new GeoPoint(-12.1184, -77.0297), null, clock.instant());
        for (int i = 0; i < count; i++) planner.offerPublished(offer);
    }
    @Then("the consumer receives {int} recorded web push")
    public void received(int count) { assertThat(push.messages()).hasSize(count); }
}
