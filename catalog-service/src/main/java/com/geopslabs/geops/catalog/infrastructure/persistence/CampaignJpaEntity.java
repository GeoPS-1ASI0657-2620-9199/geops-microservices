package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.CampaignStatus;
import com.geopslabs.geops.catalog.domain.models.ZoneType;
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

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
public class CampaignJpaEntity {
    private static final int NAME_LENGTH = 150;
    private static final int STATUS_LENGTH = 20;
    private static final int ZONE_TYPE_LENGTH = 10;
    private static final int DISTRICT_LENGTH = 100;
    private static final int AMOUNT_PRECISION = 10;
    private static final int AMOUNT_SCALE = 2;
    private static final String TEXT = "text";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "name", nullable = false, length = NAME_LENGTH)
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = TEXT)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = STATUS_LENGTH)
    private CampaignStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_type", nullable = false, length = ZONE_TYPE_LENGTH)
    private ZoneType zoneType;

    @Column(name = "zone_radius_m")
    private Integer zoneRadiusMeters;

    @Column(name = "zone_district", length = DISTRICT_LENGTH)
    private String zoneDistrict;

    @Column(name = "estimated_budget", nullable = false, precision = AMOUNT_PRECISION, scale = AMOUNT_SCALE)
    private BigDecimal estimatedBudget;
}
