package com.geopslabs.geops.reservation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reservations", indexes = {
    @Index(name = "idx_consumer_id", columnList = "consumer_id"),
    @Index(name = "idx_payment_id", columnList = "payment_id"),
    @Index(name = "idx_code", columnList = "code"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Getter
@Setter
public class ReservationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumer_id", nullable = false)
    private Long consumerId;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "payment_code", nullable = false)
    private String paymentCode;

    @Column(name = "product_type")
    private String productType;

    @Column(name = "offer_id")
    private Long offerId;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "expires_at")
    private String expiresAt;
}
