package com.geopslabs.geops.notification.application.ports;

public interface EmailSender { void send(String to, String subject, String htmlBody); }
