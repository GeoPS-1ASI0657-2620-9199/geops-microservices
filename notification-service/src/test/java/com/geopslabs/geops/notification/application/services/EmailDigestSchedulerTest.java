package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.*;
import com.geopslabs.geops.notification.infrastructure.channels.SmtpEmailChannel;
import com.icegreen.greenmail.util.*;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailDigestSchedulerTest {
    @Test
    void threeOffersArriveAsOneEmailWithTypeSpecificUnsubscribeLinks() throws Exception {
        var greenmail = new GreenMail(new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP)); greenmail.start();
        try {
            var mail = new JavaMailSenderImpl(); mail.setHost("127.0.0.1"); mail.setPort(greenmail.getSmtp().getPort());
            var digests = mock(EmailDigestRepositoryPort.class); var notices = mock(NoticePlanningRepositoryPort.class); var unsubscriptions = mock(EmailUnsubscriptionRepositoryPort.class);
            var clock = Clock.fixed(Instant.parse("2026-10-27T16:01:00Z"), ZoneOffset.UTC);
            var digest = new EmailDigest(1L, 2001L, clock.instant().minusSeconds(60), List.of(1052L, 1053L, 1054L));
            doAnswer(i -> { ((Runnable) i.getArgument(0)).run(); return null; }).when(digests).withSchedulerLock(any());
            when(digests.due(any())).thenReturn(List.of(digest));
            when(notices.lockPreferences(2001L)).thenReturn(Optional.of(new NotificationPreference(2001L, false, true, 3, LocalDateTime.now(clock))));
            when(notices.recipient(2001L)).thenReturn(Optional.of(new Recipient(2001L, "consumer@example.test", true, "CONSUMER")));
            when(notices.hasFreshLocation(eq(2001L), any())).thenReturn(true);
            var pending = new ArrayList<Notification>();
            for (long id : digest.offerIds()) pending.add(new Notification(id, 2001L, NotificationType.NEARBY_OFFER, Channel.EMAIL, "Oferta", "Offer " + id + " <script>, a 300 m, válida hasta el 15/11.", Long.toString(id), DeliveryStatus.PENDING, LocalDateTime.now(clock)));
            when(digests.notices(digest)).thenReturn(pending);
            new EmailDigestScheduler(digests, notices, unsubscriptions, new SmtpEmailChannel(mail, "avisos@geops.local"), new TestTransactions(), clock, "test-key", "http://localhost:4200", "<ul>{{offers}}</ul>{{unsubscribeLinks}}").sendDue();
            assertThat(greenmail.waitForIncomingEmail(5000, 1)).isTrue();
            assertThat(greenmail.getReceivedMessages()).hasSize(1);
            var html = greenmail.getReceivedMessages()[0].getContent().toString();
            assertThat(html).contains("1052", "1053", "1054", "300 m", "15/11", "/baja?token=2001.NEARBY_OFFER.", "&lt;script&gt;").doesNotContain("<script>");
            verify(digests).complete(1L, clock.instant());
            verify(notices, times(3)).status(anyLong(), eq(DeliveryStatus.SENT));
        } finally { greenmail.stop(); }
    }
}
