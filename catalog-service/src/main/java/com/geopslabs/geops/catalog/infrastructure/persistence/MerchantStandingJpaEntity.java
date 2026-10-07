package com.geopslabs.geops.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "merchant_standings")
@Getter
@Setter
public class MerchantStandingJpaEntity {
    private static final int BUSINESS_NAME_LENGTH = 150;
    private static final int INDEX_PRECISION = 5;
    private static final int INDEX_SCALE = 2;

    @Id
    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "business_name", nullable = false, length = BUSINESS_NAME_LENGTH)
    private String businessName;

    @Column(name = "ruc_verified", nullable = false)
    private boolean rucVerified;

    @Column(name = "open_reports", nullable = false)
    private int openReports;

    @Column(name = "compliance_index", nullable = false, precision = INDEX_PRECISION, scale = INDEX_SCALE)
    private BigDecimal complianceIndex;

    @Column(name = "verified_seal", nullable = false)
    private boolean verifiedSeal;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
