package com.geopslabs.geops.notification.infrastructure.channels;

import com.geopslabs.geops.notification.application.exceptions.ProviderUnavailableException;
import com.geopslabs.geops.notification.application.ports.*;
import com.geopslabs.geops.notification.domain.models.*;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.*;
import org.springframework.mail.javamail.*;
import org.springframework.web.util.HtmlUtils;
import java.nio.charset.StandardCharsets;

/**
 * E-mail through SMTP (US47). Each delivery goes through the circuit breaker and the bulkhead "smtp" (US34): with
 * the circuit open or the bulkhead full the server is not called and the send fails at once with
 * ProviderUnavailableException, so the digest stays due for the next round and a report notice is retried.
 */
public class SmtpEmailChannel implements EmailSender, NotificationChannelStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpEmailChannel.class);
    private static final String PROVIDER = "smtp";
    private final JavaMailSender mail;
    private final String from;
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;
    public SmtpEmailChannel(JavaMailSender mail, String from, CircuitBreaker circuitBreaker, Bulkhead bulkhead) {
        this.mail = mail; this.from = from; this.circuitBreaker = circuitBreaker; this.bulkhead = bulkhead;
    }
    @Override
    public Channel channel() { return Channel.EMAIL; }
    @Override
    public DeliveryStatus send(Notification notification, Recipient recipient) {
        try {
            send(recipient.email(), notification.getTitle(), "<p>" + HtmlUtils.htmlEscape(notification.getMessage()) + "</p>");
            LOGGER.info("notification.email.sent recipientId={}", recipient.userId());
            return DeliveryStatus.SENT;
        } catch (ProviderUnavailableException error) {
            throw error;
        } catch (RuntimeException error) {
            LOGGER.warn("notification.email.failed recipientId={} reason={}", recipient.userId(), error.getClass().getSimpleName());
            return DeliveryStatus.FAILED;
        }
    }
    @Override
    public void send(String to, String subject, String htmlBody) {
        var message = mail.createMimeMessage();
        try {
            var helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from); helper.setTo(to); helper.setSubject(subject); helper.setText(htmlBody, true);
        } catch (jakarta.mail.MessagingException error) { throw new IllegalStateException("Email message could not be prepared", error); }
        try {
            circuitBreaker.executeRunnable(() -> bulkhead.executeRunnable(() -> mail.send(message)));
        } catch (CallNotPermittedException error) {
            LOGGER.warn("notification.email.rejected reason=circuit-open");
            throw new ProviderUnavailableException(PROVIDER, "circuit-open");
        } catch (BulkheadFullException error) {
            LOGGER.warn("notification.email.rejected reason=bulkhead-full");
            throw new ProviderUnavailableException(PROVIDER, "bulkhead-full");
        }
    }
}
