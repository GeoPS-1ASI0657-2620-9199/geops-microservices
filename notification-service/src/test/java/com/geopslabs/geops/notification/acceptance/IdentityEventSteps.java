package com.geopslabs.geops.notification.acceptance;

import com.geopslabs.geops.notification.application.services.NotificationPreferenceQueryService;
import com.geopslabs.geops.notification.infrastructure.messaging.EventFixtures;
import com.geopslabs.geops.notification.infrastructure.messaging.IdentityEventsListener;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * US28: the events of identity.events go through the real listener, with the examples of the contract. Kafka is off
 * in the acceptance tests, so the step hands the record to the listener as the container would.
 */
public class IdentityEventSteps {
    private static final String TOPIC = "identity.events";
    private static final String OWN_PACKAGE = "com.geopslabs.geops.notification";
    private static final String TOKEN = "q3Zt8b1xV0mWc9yRkP2sLhN4dF6gJ7aE5uT0iO3pQ1w";

    private final IdentityEventsListener listener;
    private final RecordingEmailSender email;
    private final JdbcTemplate jdbc;
    private JavaClasses classes;

    public IdentityEventSteps(IdentityEventsListener listener, RecordingEmailSender email, JdbcTemplate jdbc) {
        this.listener = listener;
        this.email = email;
        this.jdbc = jdbc;
    }

    @Given("Identity publishes UserRegistered for consumer {long} with a confirmation link")
    @When("Identity publishes UserRegistered for consumer {long} with a confirmation link again")
    public void userRegistered(Long consumerId) {
        deliver(EventFixtures.withEventId("UserRegistered", UUID.randomUUID()), consumerId);
    }

    @When("Identity publishes EmailConfirmed for consumer {long}")
    public void emailConfirmed(Long consumerId) {
        deliver(EventFixtures.withEventId("EmailConfirmed", UUID.randomUUID()), consumerId);
    }

    @Then("consumer {long} has a recipient copy with the email {word}")
    public void recipientCopy(Long consumerId, String state) {
        var confirmed = jdbc.queryForObject("SELECT email_confirmed FROM recipients WHERE user_id = ?", Boolean.class,
                consumerId);
        assertThat(confirmed).isEqualTo("confirmed".equals(state));
    }

    @And("consumer {long} has both offer channels off and a limit of {int}")
    public void defaultPreferences(Long consumerId, int limit) {
        var row = jdbc.queryForMap("SELECT push_enabled, email_enabled, daily_limit FROM notification_preferences "
                + "WHERE consumer_id = ?", consumerId);
        assertThat(row.get("push_enabled")).isEqualTo(false);
        assertThat(row.get("email_enabled")).isEqualTo(false);
        assertThat(((Number) row.get("daily_limit")).intValue()).isEqualTo(limit);
    }

    @And("{int} confirmation email with the link of the account is sent")
    public void confirmationEmails(int count) {
        assertThat(email.messages()).hasSize(count);
        email.messages().forEach(message -> assertThat(message).contains("/confirmar-correo?token=" + TOKEN));
    }

    @When("the import graph of Notification is analyzed")
    public void theImportGraphIsAnalyzed() {
        classes = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(OWN_PACKAGE);
    }

    @Then("no class of Notification depends on Identity")
    public void noDependencyOnIdentity() {
        noClasses().that().resideInAPackage(OWN_PACKAGE + "..")
                .should().dependOnClassesThat().resideInAPackage("com.geopslabs.geops.identity..")
                .check(classes);
    }

    @And("no class of Notification makes an HTTP call to another service")
    public void noHttpCalls() {
        noClasses().that().resideInAPackage(OWN_PACKAGE + "..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web.client..",
                        "org.springframework.web.reactive.function.client..", "java.net.http..")
                .check(classes);
    }

    private void deliver(String value, Long consumerId) {
        listener.onEvent(new ConsumerRecord<>(TOPIC, 0, 0L, consumerId.toString(), value), () -> { });
    }
}
