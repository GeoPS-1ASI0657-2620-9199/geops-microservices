package com.geopslabs.geops.catalog.domain.models;

import java.time.LocalDate;

public class Campaign {
    private final Long id;
    private final Long businessId;
    private final String name;
    private final String description;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final ECampaignStatus status;
    private final float estimatedBudget;

    @SuppressWarnings("java:S107")
    public Campaign(Long id, Long businessId, String name, String description, LocalDate startDate, LocalDate endDate,
                    ECampaignStatus status, float estimatedBudget) {
        this.id = id;
        this.businessId = businessId;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.estimatedBudget = estimatedBudget;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public ECampaignStatus getStatus() {
        return status;
    }

    public float getEstimatedBudget() {
        return estimatedBudget;
    }
}
