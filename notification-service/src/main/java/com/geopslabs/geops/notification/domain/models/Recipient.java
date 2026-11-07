package com.geopslabs.geops.notification.domain.models;

public record Recipient(Long userId, String email, Boolean emailConfirmed, String role, Long businessId) {
    public static final String CONSUMER = "CONSUMER";
    public Recipient(Long userId, String email, Boolean emailConfirmed, String role) { this(userId, email, emailConfirmed, role, null); }
}
