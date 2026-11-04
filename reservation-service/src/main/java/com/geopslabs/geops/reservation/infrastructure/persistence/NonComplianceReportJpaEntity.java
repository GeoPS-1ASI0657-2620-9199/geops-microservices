package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.models.ReportResolution;
import com.geopslabs.geops.reservation.domain.models.ReportStatus;
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
@Table(name = "noncompliance_reports")
@Getter
@Setter
public class NonComplianceReportJpaEntity {
    private static final int REASON_LENGTH = 500;
    private static final int ENUM_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false, unique = true)
    private Long reservationId;

    @Column(name = "consumer_id", nullable = false)
    private Long consumerId;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "reason", nullable = false, length = REASON_LENGTH)
    private String reason;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "response_deadline", nullable = false)
    private Instant responseDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = ENUM_LENGTH)
    private ReportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution", length = ENUM_LENGTH)
    private ReportResolution resolution;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
