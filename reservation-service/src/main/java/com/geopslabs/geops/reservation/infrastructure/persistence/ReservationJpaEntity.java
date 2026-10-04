package com.geopslabs.geops.reservation.infrastructure.persistence;

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
@Table(name = "reservations")
@Getter
@Setter
public class ReservationJpaEntity {
    private static final int CODE_LENGTH = 12;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumer_id", nullable = false)
    private Long consumerId;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Column(name = "code", nullable = false, unique = true, length = CODE_LENGTH)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
