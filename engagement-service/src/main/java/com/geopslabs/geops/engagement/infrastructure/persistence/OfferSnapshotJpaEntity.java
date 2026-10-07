package com.geopslabs.geops.engagement.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "offer_snapshots")
@Getter
@Setter
public class OfferSnapshotJpaEntity {
    private static final int TITLE_LENGTH = 255;
    private static final int STATUS_LENGTH = 20;

    @Id
    @Column(name = "offer_id")
    private Long offerId;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "title", nullable = false, length = TITLE_LENGTH)
    private String title;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(name = "status", nullable = false, length = STATUS_LENGTH)
    private String status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
