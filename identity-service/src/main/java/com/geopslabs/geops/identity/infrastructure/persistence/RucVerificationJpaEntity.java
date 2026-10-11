package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.VerificationResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * One row per verification attempt. rejection_reason keeps the reason code, also for a PENDING attempt.
 */
@Entity
@Table(name = "ruc_verifications")
@Getter
@Setter
@NoArgsConstructor
public class RucVerificationJpaEntity {
    private static final int RUC_LENGTH = 11;
    private static final int TAXPAYER_FIELD_LENGTH = 30;
    private static final int RESULT_LENGTH = 20;
    private static final int REASON_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_profile_id", nullable = false)
    private Long businessProfileId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ruc", nullable = false, length = RUC_LENGTH)
    private String ruc;

    @Column(name = "taxpayer_status", length = TAXPAYER_FIELD_LENGTH)
    private String taxpayerStatus;

    @Column(name = "taxpayer_condition", length = TAXPAYER_FIELD_LENGTH)
    private String taxpayerCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = RESULT_LENGTH)
    private VerificationResult result;

    @Column(name = "rejection_reason", length = REASON_LENGTH)
    private String rejectionReason;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;
}
