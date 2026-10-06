package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.commands.CreateCampaignCommand;

import java.time.LocalDate;

public class Campaign {
    private Long id;
    private Long businessId;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private ECampaignStatus status;
    private float estimatedBudget;

    public Campaign(CreateCampaignCommand command) {
        this.businessId = command.businessId();
        this.name = command.name();
        this.description = command.description();
        this.startDate = command.startDate();
        this.endDate = command.endDate();
        this.status = ECampaignStatus.ACTIVE;
        this.estimatedBudget = command.estimatedBudget() != null ? command.estimatedBudget() : 0;
    }

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

    public void edit(String name, String description, LocalDate startDate, LocalDate endDate, ECampaignStatus status,
                     Float estimatedBudget) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        if (estimatedBudget != null) {
            this.estimatedBudget = estimatedBudget;
        }
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
