package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.backend.identity.domain.model.aggregates.User;
import com.geopslabs.geops.backend.payments.domain.model.aggregates.Payment;
import com.geopslabs.geops.reservation.shared.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reservations", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_payment_id", columnList = "payment_id"),
    @Index(name = "idx_code", columnList = "code"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Getter
@Setter
public class ReservationJpaEntity extends AuditableAbstractAggregateRoot<ReservationJpaEntity> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

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
