package com.geopslabs.geops.notification.infrastructure.channels;

import com.icegreen.greenmail.util.*;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import static org.assertj.core.api.Assertions.*;
class SmtpEmailChannelTest {
    @Test
    void sendsUtf8HtmlWithSubjectAndSenderThroughSmtp() throws Exception {
        var greenmail = new GreenMail(new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
        greenmail.start();
        try {
            var mail = new JavaMailSenderImpl(); mail.setHost("127.0.0.1"); mail.setPort(greenmail.getSmtp().getPort());
            new SmtpEmailChannel(mail, "avisos@geops.local").send("consumer@example.test", "Oferta cerca de ti", "<p>Menú, a 300 m, válida hasta el 15/11.</p>");
            assertThat(greenmail.waitForIncomingEmail(5000, 1)).isTrue();
            var message = greenmail.getReceivedMessages()[0];
            assertThat(message.getSubject()).isEqualTo("Oferta cerca de ti");
            assertThat(message.getFrom()[0].toString()).isEqualTo("avisos@geops.local");
            assertThat(message.getContent().toString()).contains("Menú", "300 m", "15/11");
        } finally { greenmail.stop(); }
    }
}
