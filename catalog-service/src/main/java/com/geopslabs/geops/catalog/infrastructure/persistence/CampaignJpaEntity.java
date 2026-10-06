package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.backend.identity.domain.model.aggregates.User;
import com.geopslabs.geops.catalog.domain.models.ECampaignStatus;
import com.geopslabs.geops.catalog.shared.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "campaign")
@Getter
@Setter
public class CampaignJpaEntity extends AuditableAbstractAggregateRoot<CampaignJpaEntity> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ECampaignStatus status;

    @Column(name = "estimated_budget", nullable = false)
    private float estimatedBudget;

    @Column(name = "total_impressions", nullable = false)
    private Long totalImpressions;

    @Column(name = "total_clicks", nullable = false)
    private Long totalClicks;

    @Column(name = "CTR", nullable = false)
    private float ctr;
}
