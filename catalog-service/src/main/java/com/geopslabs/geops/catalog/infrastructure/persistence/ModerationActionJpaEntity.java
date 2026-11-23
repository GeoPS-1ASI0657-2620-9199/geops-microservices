package com.geopslabs.geops.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "moderation_actions")
@Getter
@Setter
public class ModerationActionJpaEntity {
    private static final int REASON_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "reason", nullable = false, length = REASON_LENGTH)
    private String reason;

    @Column(name = "removed_at", nullable = false)
    private Instant removedAt;
}
