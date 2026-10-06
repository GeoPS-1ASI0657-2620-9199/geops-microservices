package com.geopslabs.geops.catalog.domain.models;

public class Campaign {
    private final Long id;
    private final Long businessId;
    private final String name;
    private final String description;
    private final DateRange period;
    private final CampaignZone zone;
    private final CampaignStatus status;
    private final Money estimatedBudget;

    @SuppressWarnings("java:S107")
    public Campaign(Long id, Long businessId, String name, String description, DateRange period, CampaignZone zone,
                    CampaignStatus status, Money estimatedBudget) {
        this.id = id;
        this.businessId = businessId;
        this.name = name;
        this.description = description;
        this.period = period;
        this.zone = zone;
        this.status = status;
        this.estimatedBudget = estimatedBudget;
    }

    public boolean isOfBusiness(Long candidateBusinessId) {
        return businessId.equals(candidateBusinessId);
    }

    public Long getId() {
        return id;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public DateRange getPeriod() {
        return period;
    }

    public CampaignZone getZone() {
        return zone;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public Money getEstimatedBudget() {
        return estimatedBudget;
    }
}
