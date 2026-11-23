package com.geopslabs.geops.notification.infrastructure.channels;

import com.geopslabs.geops.notification.application.exceptions.ProviderUnavailableException;
import com.geopslabs.geops.notification.domain.models.*;
import com.icegreen.greenmail.util.*;
import com.icegreen.greenmail.util.ServerSetup;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.time.LocalDateTime;
import java.util.Properties;
import static com.geopslabs.geops.notification.infrastructure.channels.ProviderResilience.FAILURES_THAT_OPEN;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class SmtpEmailChannelTest {
    private static final String TO = "consumer@example.test";
    private final CircuitBreaker circuitBreaker = ProviderResilience.circuitBreaker("smtp");
    private final Bulkhead bulkhead = ProviderResilience.singlePlaceBulkhead("smtp");
    private final JavaMailSender mail = mock(JavaMailSender.class);
    private final Recipient recipient = new Recipient(2001L, TO, true, "CONSUMER");
    private final Notification notice = new Notification(1L, 2001L, NotificationType.REPORT_RECEIVED, Channel.EMAIL, "Reporte", "Tienes un reporte", "report-7", DeliveryStatus.PENDING, LocalDateTime.now());
    @Test
    void sendsUtf8HtmlWithSubjectAndSenderThroughSmtp() throws Exception {
        var greenmail = new GreenMail(new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
        greenmail.start();
        try {
            var mail = new JavaMailSenderImpl(); mail.setHost("127.0.0.1"); mail.setPort(greenmail.getSmtp().getPort());
            new SmtpEmailChannel(mail, "avisos@geops.local", circuitBreaker, bulkhead).send(TO, "Oferta cerca de ti", "<p>Menú, a 300 m, válida hasta el 15/11.</p>");
            assertThat(greenmail.waitForIncomingEmail(5000, 1)).isTrue();
            var message = greenmail.getReceivedMessages()[0];
            assertThat(message.getSubject()).isEqualTo("Oferta cerca de ti");
            assertThat(message.getFrom()[0].toString()).isEqualTo("avisos@geops.local");
            assertThat(message.getContent().toString()).contains("Menú", "300 m", "15/11");
        } finally { greenmail.stop(); }
    }
    @Test
    void fiveSmtpFailuresOpenTheCircuitAndTheNextSendIsRejectedWithoutCallingTheServer() {
        serverDown();
        var channel = channel();
        for (int attempt = 0; attempt < FAILURES_THAT_OPEN; attempt++) assertThatThrownBy(() -> channel.send(TO, "Ofertas", "<p>1</p>")).isInstanceOf(MailSendException.class);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThatThrownBy(() -> channel.send(TO, "Ofertas", "<p>1</p>")).isInstanceOf(ProviderUnavailableException.class);
        verify(mail, times(FAILURES_THAT_OPEN)).send(any(MimeMessage.class));
    }
    @Test
    void anOrdinaryFailureOfAReportNoticeIsFailedButAnOpenCircuitLeavesItPendingForRetry() {
        serverDown();
        var channel = channel();
        assertThat(channel.send(notice, recipient)).isEqualTo(DeliveryStatus.FAILED);

        circuitBreaker.transitionToOpenState();
        assertThatThrownBy(() -> channel.send(notice, recipient)).isInstanceOf(ProviderUnavailableException.class);
    }
    @Test
    void aSuccessfulProbeClosesTheCircuitAgain() {
        when(mail.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        circuitBreaker.transitionToOpenState(); circuitBreaker.transitionToHalfOpenState();

        assertThat(channel().send(notice, recipient)).isEqualTo(DeliveryStatus.SENT);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
    @Test
    void aFullBulkheadRejectsTheSendAtOnceWithoutOpeningTheCircuit() {
        when(mail.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        assertThat(bulkhead.tryAcquirePermission()).isTrue();

        assertThatThrownBy(() -> channel().send(TO, "Ofertas", "<p>1</p>")).isInstanceOf(ProviderUnavailableException.class);
        verify(mail, never()).send(any(MimeMessage.class));
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }
    private SmtpEmailChannel channel() { return new SmtpEmailChannel(mail, "avisos@geops.local", circuitBreaker, bulkhead); }
    private void serverDown() {
        when(mail.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("connection refused")).when(mail).send(any(MimeMessage.class));
    }
}
