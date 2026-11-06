package com.geopslabs.geops.notification.infrastructure.channels;

import com.geopslabs.geops.notification.application.ports.*;
import com.geopslabs.geops.notification.domain.models.*;
import org.slf4j.*;
import org.springframework.mail.javamail.*;
import org.springframework.web.util.HtmlUtils;
import java.nio.charset.StandardCharsets;

public class SmtpEmailChannel implements EmailSender, NotificationChannelStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpEmailChannel.class);
    private final JavaMailSender mail;
    private final String from;
    public SmtpEmailChannel(JavaMailSender mail, String from) { this.mail = mail; this.from = from; }
    @Override
    public Channel channel() { return Channel.EMAIL; }
    @Override
    public DeliveryStatus send(Notification notification, Recipient recipient) {
        try {
            send(recipient.email(), notification.getTitle(), "<p>" + HtmlUtils.htmlEscape(notification.getMessage()) + "</p>");
            LOGGER.info("notification.email.sent recipientId={}", recipient.userId());
            return DeliveryStatus.SENT;
        } catch (RuntimeException error) {
            LOGGER.warn("notification.email.failed recipientId={} reason={}", recipient.userId(), error.getClass().getSimpleName());
            return DeliveryStatus.FAILED;
        }
    }
    @Override
    public void send(String to, String subject, String htmlBody) {
        try {
            var message = mail.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from); helper.setTo(to); helper.setSubject(subject); helper.setText(htmlBody, true);
            mail.send(message);
        } catch (jakarta.mail.MessagingException error) { throw new IllegalStateException("Email message could not be prepared", error); }
    }
}
