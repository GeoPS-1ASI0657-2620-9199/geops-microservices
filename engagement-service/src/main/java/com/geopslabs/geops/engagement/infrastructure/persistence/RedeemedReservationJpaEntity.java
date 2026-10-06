package com.geopslabs.geops.engagement.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "redeemed_reservations")
@Getter
@Setter
public class RedeemedReservationJpaEntity {

    @Id
    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "consumer_id", nullable = false)
    private Long consumerId;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "redeemed_at", nullable = false)
    private Instant redeemedAt;
}
