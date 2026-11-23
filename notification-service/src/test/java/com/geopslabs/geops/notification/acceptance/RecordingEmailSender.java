package com.geopslabs.geops.notification.acceptance;

import com.geopslabs.geops.notification.application.ports.EmailSender;
import java.util.*;
public class RecordingEmailSender implements EmailSender {
    private final List<String> messages = new ArrayList<>();
    private volatile EmailSender realSender;
    @Override public void send(String to, String subject, String htmlBody) {
        if (realSender != null) realSender.send(to, subject, htmlBody);
        messages.add(htmlBody);
    }
    /** US34: from here on the e-mail goes through the real SMTP channel, with its circuit breaker. */
    public void useRealSender(EmailSender sender) { realSender = sender; }
    public void clear() { messages.clear(); realSender = null; }
    public List<String> messages() { return List.copyOf(messages); }
}
