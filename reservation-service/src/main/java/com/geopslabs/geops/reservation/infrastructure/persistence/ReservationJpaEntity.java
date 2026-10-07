package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "reservations")
@Getter
@Setter
public class ReservationJpaEntity {
    private static final int CODE_LENGTH = 12;
    private static final int OFFER_TITLE_LENGTH = 255;
    private static final int STATUS_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = CODE_LENGTH)
    private String code;

    @Column(name = "consumer_id", nullable = false)
    private Long consumerId;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "offer_title", nullable = false, length = OFFER_TITLE_LENGTH)
    private String offerTitle;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = STATUS_LENGTH)
    private ReservationStatus status;
}
