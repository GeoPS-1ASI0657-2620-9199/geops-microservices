package com.geopslabs.geops.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * One row per token closed before its expiry, keyed by the SHA-256 of the token.
 */
@Entity
@Table(name = "revoked_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RevokedTokenJpaEntity {
    private static final int FINGERPRINT_LENGTH = 64;

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "fingerprint", length = FINGERPRINT_LENGTH, nullable = false)
    private String fingerprint;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;
}
