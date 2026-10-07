package com.geopslabs.geops.notification.domain.models;

public record Recipient(Long userId, String email, Boolean emailConfirmed, String role) {
    public static final String CONSUMER = "CONSUMER";
}
