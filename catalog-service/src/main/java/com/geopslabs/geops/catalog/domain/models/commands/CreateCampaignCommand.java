package com.geopslabs.geops.catalog.domain.models.commands;

import com.geopslabs.geops.catalog.domain.models.Campaign;

import java.time.LocalDate;

public record CreateCampaignCommand(Long businessId, String name, String description, LocalDate startDate, LocalDate endDate, Float estimatedBudget) {
    public CreateCampaignCommand {
        if(businessId == null || businessId < 0)
            throw new IllegalArgumentException("businessId cannot be null");

        if(name == null || name.isBlank())
            throw new IllegalArgumentException("Campaign name cannot be null or blank");

        if(description == null || description.isBlank())
            throw new IllegalArgumentException("Campaign description cannot be null or blank");

        if(startDate == null)
            throw new IllegalArgumentException("Start date cannot be null");

        if(endDate == null)
            throw new IllegalArgumentException("End date cannot be null");

        if(endDate.isBefore(startDate))
            throw new IllegalArgumentException("End date cannot be before start date");
    }
}
