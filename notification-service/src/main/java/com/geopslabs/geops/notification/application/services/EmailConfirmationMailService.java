package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.application.ports.EmailSender;
import com.geopslabs.geops.notification.application.usecases.SendEmailConfirmationUseCase;
import com.geopslabs.geops.notification.domain.models.ConfirmationLink;
import com.geopslabs.geops.notification.domain.ports.ConfirmationEmailRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Sends the confirmation email of a new account from UserRegistered (US28). The record of the sending and the email
 * go together: if the email fails the record is rolled back and the event is retried, and a repeated event finds
 * the record and sends nothing. An expired link is not sent.
 */
public class EmailConfirmationMailService implements SendEmailConfirmationUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailConfirmationMailService.class);
    static final String SUBJECT = "Confirma tu correo en GeoPS";
    private static final String CONFIRMATION_PATH = "/confirmar-correo";
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter EXPIRES_AT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ConfirmationEmailRepositoryPort confirmationEmails;
    private final EmailSender sender;
    private final Clock clock;
    private final String webBaseUrl;
    private final String template;

    public EmailConfirmationMailService(ConfirmationEmailRepositoryPort confirmationEmails, EmailSender sender,
                                        Clock clock, String webBaseUrl, String template) {
        this.confirmationEmails = confirmationEmails;
        this.sender = sender;
        this.clock = clock;
        this.webBaseUrl = webBaseUrl;
        this.template = template;
    }

    @Override
    @Transactional
    public void send(ConfirmationLink link) {
        var now = clock.instant();
        if (link.isExpiredAt(now)) {
            LOGGER.info("notification.confirmation-email.skipped userId={} reason=expired-link", link.userId());
            return;
        }
        if (!confirmationEmails.claim(link.userId(), now)) {
            LOGGER.info("notification.confirmation-email.skipped userId={} reason=already-sent", link.userId());
            return;
        }
        sender.send(link.email(), SUBJECT, body(link));
        LOGGER.info("notification.confirmation-email.sent userId={}", link.userId());
    }

    private String body(ConfirmationLink link) {
        var url = UriComponentsBuilder.fromUriString(webBaseUrl).path(CONFIRMATION_PATH)
                .queryParam("token", link.token()).encode().build().toUriString();
        return template.replace("{{link}}", HtmlUtils.htmlEscape(url))
                .replace("{{expiresAt}}", EXPIRES_AT.format(link.expiresAt().atZone(LIMA)));
    }
}
