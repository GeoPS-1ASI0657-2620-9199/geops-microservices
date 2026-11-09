package com.geopslabs.geops.notification.acceptance;

import com.geopslabs.geops.notification.application.ports.EmailSender;
import java.util.*;
public class RecordingEmailSender implements EmailSender {
    private final List<String> messages = new ArrayList<>();
    @Override public void send(String to, String subject, String htmlBody) { messages.add(htmlBody); }
    public void clear() { messages.clear(); }
    public List<String> messages() { return List.copyOf(messages); }
}
