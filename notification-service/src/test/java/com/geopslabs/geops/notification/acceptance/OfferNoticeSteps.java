package com.geopslabs.geops.notification.acceptance;

import com.geopslabs.geops.notification.application.services.*;
import com.geopslabs.geops.notification.application.usecases.PlanOfferNoticesUseCase;
import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.*;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

public class OfferNoticeSteps {
    private final PlanOfferNoticesUseCase planner;
    private final RecipientRepositoryPort recipients;
    private final NotificationPreferenceRepositoryPort preferences;
    private final LastKnownLocationRepositoryPort locations;
    private final EmailUnsubscriptionService unsubscription;
    private final EmailDigestScheduler scheduler;
    private final MutableClock clock;
    private final RecordingEmailSender email;
    private final JdbcTemplate jdbc;
    private final String secret;
    public OfferNoticeSteps(PlanOfferNoticesUseCase planner, RecipientRepositoryPort recipients, NotificationPreferenceRepositoryPort preferences,
                           LastKnownLocationRepositoryPort locations, EmailUnsubscriptionService unsubscription, EmailDigestScheduler scheduler,
                           MutableClock clock, RecordingEmailSender email, JdbcTemplate jdbc, @Value("${geops.notifications.unsubscribe-secret}") String secret) {
        this.planner = planner; this.recipients = recipients; this.preferences = preferences; this.locations = locations; this.unsubscription = unsubscription;
        this.scheduler = scheduler; this.clock = clock; this.email = email; this.jdbc = jdbc; this.secret = secret;
    }
    @Before(order = -10)
    public void reset() { clock.reset(); email.clear(); }
    @Given("consumer {long} has a confirmed email with email on and a limit of {int}")
    public void consumer(Long id, int limit) {
        recipients.upsert(new Recipient(id, "consumer@example.test", true, "CONSUMER"));
        preferences.upsert(new NotificationPreference(id, false, true, limit, LocalDateTime.now(clock)));
        locations.saveIfNewer(id, new LastKnownLocation(new GeoPoint(-12.1211, -77.0297), 20, 800, LocalDateTime.now(clock).minusMinutes(20)));
    }
    @Given("consumer {long} turned both offer channels off")
    public void off(Long id) { preferences.upsert(new NotificationPreference(id, false, false, 3, LocalDateTime.now(clock))); }
    @Given("consumer {long} used the nearby email unsubscribe link")
    public void unsubscribe(Long id) { unsubscription.unsubscribe(UnsubscribeToken.issue(id, NotificationType.NEARBY_OFFER, secret)); }
    @When("Catalog publishes {int} nearby offers in the same window")
    public void publish(int count) {
        for (long id = 1052; id < 1052 + count; id++) planner.offerPublished(new OfferPublishedEvent(id, 84L, "Don Pepe", "Oferta " + id, LocalDate.parse("2026-11-15"), new GeoPoint(-12.1184, -77.0297), null, clock.instant()));
    }
    @When("the email window closes")
    public void close() { clock.advance(Duration.ofMinutes(3)); scheduler.sendDue(); }
    @Then("the consumer receives {int} grouped email with {int} offers")
    public void received(int count, int offers) {
        assertThat(email.messages()).hasSize(count);
        if (count > 0) assertThat(email.messages().get(0).split("<li>").length - 1).isEqualTo(offers);
    }
    @Then("the grouped email includes the offer distance validity and unsubscribe link")
    public void details() { assertThat(email.messages().get(0)).contains("Oferta 1052", " m", "15/11", "/baja?token=2001.NEARBY_OFFER."); }
    @Then("consumer {long} has {long} pending notices and {long} skipped by the limit")
    public void quota(Long consumerId, long pending, long skipped) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id = ? AND delivery_status = 'PENDING'", Long.class, consumerId)).isEqualTo(pending);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id = ? AND delivery_status = 'SKIPPED_BY_LIMIT'", Long.class, consumerId)).isEqualTo(skipped);
    }
    @Then("consumer {long} has no offer notices")
    public void none(Long consumerId) { assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id = ?", Long.class, consumerId)).isZero(); }
}
