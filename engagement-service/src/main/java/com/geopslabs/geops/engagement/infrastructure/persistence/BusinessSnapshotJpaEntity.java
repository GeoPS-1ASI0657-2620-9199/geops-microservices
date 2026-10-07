package com.geopslabs.geops.engagement.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "business_snapshots")
@Getter
@Setter
public class BusinessSnapshotJpaEntity {
    private static final int BUSINESS_NAME_LENGTH = 150;

    @Id
    @Column(name = "business_id")
    private Long businessId;

    @Column(name = "business_name", nullable = false, length = BUSINESS_NAME_LENGTH)
    private String businessName;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
