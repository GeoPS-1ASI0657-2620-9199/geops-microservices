package com.geopslabs.geops.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserJpaEntity {
    private static final int FULL_NAME_LENGTH = 255;
    private static final int EMAIL_LENGTH = 255;
    private static final int PHONE_LENGTH = 20;
    private static final int PASSWORD_HASH_LENGTH = 255;
    private static final int ROLE_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = FULL_NAME_LENGTH)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = EMAIL_LENGTH)
    private String email;

    @Column(name = "email_confirmed_at")
    private Instant emailConfirmedAt;

    @Column(name = "phone", nullable = false, unique = true, length = PHONE_LENGTH)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = PASSWORD_HASH_LENGTH)
    private String passwordHash;

    @Column(name = "role", nullable = false, length = ROLE_LENGTH)
    private String role;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
