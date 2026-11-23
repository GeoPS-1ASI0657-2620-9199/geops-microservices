package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.application.ports.EmailSender;
import com.geopslabs.geops.notification.domain.models.ConfirmationLink;
import com.geopslabs.geops.notification.domain.ports.ConfirmationEmailRepositoryPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailConfirmationMailServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-27T14:00:05Z");
    private static final String TOKEN = "q3Zt8b1xV0mWc9yRkP2sLhN4dF6gJ7aE5uT0iO3pQ1w";
    private static final ConfirmationLink LINK = new ConfirmationLink(2001L, "lucia.fernandez@ejemplo.pe", TOKEN,
            Instant.parse("2026-10-28T14:00:00Z"));

    private final ConfirmationEmailRepositoryPort confirmationEmails = mock(ConfirmationEmailRepositoryPort.class);
    private final EmailSender sender = mock(EmailSender.class);
    private final EmailConfirmationMailService service = new EmailConfirmationMailService(confirmationEmails, sender,
            Clock.fixed(NOW, ZoneOffset.UTC), "https://geops.example.test",
            "<a href=\"{{link}}\">Confirmar mi correo</a> vence el {{expiresAt}}");

    @Test
    void theFirstUserRegisteredSendsTheLinkToTheWebPage() {
        when(confirmationEmails.claim(2001L, NOW)).thenReturn(true);
        var body = ArgumentCaptor.forClass(String.class);

        service.send(LINK);

        verify(sender).send(eq("lucia.fernandez@ejemplo.pe"), eq(EmailConfirmationMailService.SUBJECT), body.capture());
        assertThat(body.getValue()).contains("https://geops.example.test/confirmar-correo?token=" + TOKEN,
                "vence el 28/10/2026 09:00");
    }

    @Test
    void aRepeatedEventDoesNotSendASecondEmail() {
        when(confirmationEmails.claim(2001L, NOW)).thenReturn(false);

        service.send(LINK);

        verify(sender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void anExpiredLinkIsNotSent() {
        service.send(new ConfirmationLink(2001L, "lucia.fernandez@ejemplo.pe", TOKEN, NOW));

        verify(confirmationEmails, never()).claim(any(), any());
        verify(sender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void aFailedSendingIsRaisedSoTheEventIsRetried() {
        when(confirmationEmails.claim(2001L, NOW)).thenReturn(true);
        doThrow(new IllegalStateException("smtp down")).when(sender).send(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> service.send(LINK)).isInstanceOf(IllegalStateException.class);
    }
}
