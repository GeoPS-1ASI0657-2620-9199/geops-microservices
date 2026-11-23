package com.geopslabs.geops.notification.acceptance;

import com.geopslabs.geops.notification.application.services.EmailDigestScheduler;
import com.geopslabs.geops.notification.application.usecases.RegisterPushSubscriptionUseCase;
import com.geopslabs.geops.notification.configuration.EmailConfiguration;
import com.geopslabs.geops.notification.configuration.FirebaseConfiguration;
import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.*;
import com.geopslabs.geops.notification.infrastructure.channels.FcmWebPushChannel;
import com.geopslabs.geops.notification.infrastructure.channels.SmtpEmailChannel;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import java.time.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * US34: FCM and the mail server are replaced by doubles that can go down, behind the real channels and the circuit
 * breakers "fcm" and "smtp" that Spring built from application.yml.
 */
public class ProviderOutageSteps {
    private final RecipientRepositoryPort recipients;
    private final NotificationPreferenceRepositoryPort preferences;
    private final LastKnownLocationRepositoryPort locations;
    private final RegisterPushSubscriptionUseCase subscriptions;
    private final PushSubscriptionRepositoryPort pushSubscriptions;
    private final RecordingPushChannel push;
    private final RecordingEmailSender email;
    private final EmailDigestScheduler scheduler;
    private final MutableClock clock;
    private final CircuitBreakerRegistry circuitBreakers;
    private final BulkheadRegistry bulkheads;
    private final AtomicInteger fcmCalls = new AtomicInteger();
    private final AtomicInteger smtpCalls = new AtomicInteger();
    private final AtomicBoolean smtpDown = new AtomicBoolean();

    public ProviderOutageSteps(RecipientRepositoryPort recipients, NotificationPreferenceRepositoryPort preferences,
                               LastKnownLocationRepositoryPort locations, RegisterPushSubscriptionUseCase subscriptions,
                               PushSubscriptionRepositoryPort pushSubscriptions, RecordingPushChannel push,
                               RecordingEmailSender email, EmailDigestScheduler scheduler, MutableClock clock,
                               CircuitBreakerRegistry circuitBreakers, BulkheadRegistry bulkheads) {
        this.recipients = recipients; this.preferences = preferences; this.locations = locations;
        this.subscriptions = subscriptions; this.pushSubscriptions = pushSubscriptions; this.push = push;
        this.email = email; this.scheduler = scheduler; this.clock = clock;
        this.circuitBreakers = circuitBreakers; this.bulkheads = bulkheads;
    }

    @Before
    public void closeTheCircuits() {
        fcm().reset();
        smtp().reset();
    }

    @Given("consumer {long} allowed push and email with a confirmed address and a limit of {int}")
    public void consumerWithBothChannels(Long id, int limit) {
        recipients.upsert(new Recipient(id, "consumer@example.test", true, "CONSUMER"));
        preferences.upsert(new NotificationPreference(id, true, true, limit, LocalDateTime.now(clock)));
        subscriptions.register(id, "test-browser-token", "Test browser");
        locations.saveIfNewer(id, new LastKnownLocation(new GeoPoint(-12.1211, -77.0297), 20, 800, LocalDateTime.now(clock).minusMinutes(20)));
    }

    @Given("FCM does not answer")
    public void fcmDoesNotAnswer() throws Exception {
        var messaging = mock(FirebaseMessaging.class);
        when(messaging.send(any(Message.class))).thenAnswer(invocation -> {
            fcmCalls.incrementAndGet();
            throw new IllegalStateException("FCM read timed out");
        });
        push.useRealChannel(new FcmWebPushChannel(messaging, pushSubscriptions, "http://localhost:4200",
                fcm(), bulkheads.bulkhead(FirebaseConfiguration.FCM)));
    }

    @Given("the mail server does not answer")
    public void mailServerDoesNotAnswer() {
        smtpDown.set(true);
        var mail = mock(JavaMailSender.class);
        when(mail.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        doAnswer(invocation -> {
            smtpCalls.incrementAndGet();
            if (smtpDown.get()) throw new MailSendException("connection timed out");
            return null;
        }).when(mail).send(any(MimeMessage.class));
        email.useRealSender(new SmtpEmailChannel(mail, "avisos@geops.local", smtp(),
                bulkheads.bulkhead(EmailConfiguration.SMTP)));
    }

    @Given("the mail server answers again")
    public void mailServerAnswersAgain() {
        smtpDown.set(false);
        smtpCalls.set(0);
    }

    @Given("the open wait of the {word} circuit has passed")
    public void theOpenWaitHasPassed(String provider) {
        circuit(provider).transitionToHalfOpenState();
    }

    @When("the email window closes {int} times")
    public void theEmailWindowClosesTimes(int times) {
        for (int round = 0; round < times; round++) {
            clock.advance(Duration.ofMinutes(3));
            scheduler.sendDue();
        }
    }

    @Then("FCM was called {int} times")
    public void fcmWasCalled(int calls) { assertThat(fcmCalls.get()).isEqualTo(calls); }

    @Then("the mail server was called {int} times")
    public void mailServerWasCalled(int calls) { assertThat(smtpCalls.get()).isEqualTo(calls); }

    @Then("the {word} circuit is {word}")
    public void theCircuitIs(String provider, String state) {
        assertThat(circuit(provider).getState()).isEqualTo(CircuitBreaker.State.valueOf(state));
    }

    private CircuitBreaker circuit(String provider) {
        return switch (provider) {
            case "FCM" -> fcm();
            case "SMTP" -> smtp();
            default -> throw new IllegalArgumentException(provider);
        };
    }

    private CircuitBreaker fcm() { return circuitBreakers.circuitBreaker(FirebaseConfiguration.FCM); }

    private CircuitBreaker smtp() { return circuitBreakers.circuitBreaker(EmailConfiguration.SMTP); }
}
