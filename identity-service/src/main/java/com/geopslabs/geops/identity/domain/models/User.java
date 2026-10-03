package com.geopslabs.geops.identity.domain.models;

import java.time.Instant;

public class User {
    private static final String ADMIN_ROLE = "ADMIN";
    private static final int NO_FAILED_LOGIN_ATTEMPTS = 0;

    private Long id;
    private String fullName;
    private String email;
    private Instant emailConfirmedAt;
    private String phone;
    private String passwordHash;
    private String role;
    private int failedLoginAttempts;
    private Instant lockedUntil;
    private Instant createdAt;

    public User(String fullName, String email, String phone, String passwordHash, String role) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.failedLoginAttempts = NO_FAILED_LOGIN_ATTEMPTS;
        this.createdAt = Instant.now();
    }

    public User(Long id, User data, Instant emailConfirmedAt, int failedLoginAttempts, Instant lockedUntil,
                Instant createdAt) {
        this(data.fullName, data.email, data.phone, data.passwordHash, data.role);
        this.id = id;
        this.emailConfirmedAt = emailConfirmedAt;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
        this.createdAt = createdAt;
    }

    public void updateUser(String fullName, String email, String phone, String role) {
        this.fullName = valueOrCurrent(fullName, this.fullName);
        this.email = valueOrCurrent(email, this.email);
        this.phone = valueOrCurrent(phone, this.phone);
        this.role = valueOrCurrent(role, this.role);
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = valueOrCurrent(passwordHash, this.passwordHash);
    }

    public boolean isAdmin() {
        return ADMIN_ROLE.equalsIgnoreCase(role);
    }

    private static String valueOrCurrent(String candidate, String current) {
        return candidate != null && !candidate.isBlank() ? candidate : current;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public Instant getEmailConfirmedAt() {
        return emailConfirmedAt;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
