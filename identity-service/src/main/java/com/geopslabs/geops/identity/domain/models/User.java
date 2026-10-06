package com.geopslabs.geops.identity.domain.models;

import java.time.Duration;
import java.time.Instant;

public class User {
    private static final int NO_FAILED_LOGIN_ATTEMPTS = 0;
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private Long id;
    private String fullName;
    private String email;
    private Instant emailConfirmedAt;
    private String phone;
    private String passwordHash;
    private Role role;
    private int failedLoginAttempts;
    private Instant lockedUntil;
    private Instant createdAt;

    public User(String fullName, String email, String phone, String passwordHash, Role role) {
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

    public static User register(String fullName, Email email, String phone, String passwordHash, Role role) {
        return new User(fullName, email.value(), phone, passwordHash, role);
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    public void registerFailedLogin(Instant now) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            lockedUntil = now.plus(LOCK_DURATION);
            failedLoginAttempts = NO_FAILED_LOGIN_ATTEMPTS;
        }
    }

    public void resetFailedLogins() {
        failedLoginAttempts = NO_FAILED_LOGIN_ATTEMPTS;
        lockedUntil = null;
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

    public Role getRole() {
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
