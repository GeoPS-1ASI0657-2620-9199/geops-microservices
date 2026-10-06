package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.backend.identity.domain.model.aggregates.User;
import com.geopslabs.geops.catalog.domain.models.commands.CreateCampaignCommand;

import java.time.LocalDate;
import java.util.Date;

public class Campaign {
    private Long id;
    private User user;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private ECampaignStatus status;
    private float estimatedBudget;
    private Long totalImpressions;
    private Long totalClicks;
    private float CTR;
    private Date createdAt;
    private Date updatedAt;

    public Campaign(User user, CreateCampaignCommand command) {
        this.user = user;
        this.name = command.name();
        this.description = command.description();
        this.startDate = command.startDate();
        this.endDate = command.endDate();
        this.status = ECampaignStatus.ACTIVE;
        this.estimatedBudget = command.estimatedBudget() != null ? command.estimatedBudget() : 0;
        this.totalImpressions = 0L;
        this.totalClicks = 0L;
        this.CTR = 0;
    }

    @SuppressWarnings("java:S107")
    public Campaign(Long id, User user, String name, String description, LocalDate startDate, LocalDate endDate,
                    ECampaignStatus status, float estimatedBudget, Long totalImpressions, Long totalClicks,
                    float ctr, Date createdAt, Date updatedAt) {
        this.id = id;
        this.user = user;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.estimatedBudget = estimatedBudget;
        this.totalImpressions = totalImpressions;
        this.totalClicks = totalClicks;
        this.CTR = ctr;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void edit(String name, String description, LocalDate startDate, LocalDate endDate, ECampaignStatus status,
                     Float estimatedBudget, Long totalImpressions, Long totalClicks, Float ctr) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        if (estimatedBudget != null) {
            this.estimatedBudget = estimatedBudget;
        }
        if (totalImpressions != null) {
            this.totalImpressions = Math.max(0L, totalImpressions);
        }
        if (totalClicks != null) {
            this.totalClicks = Math.max(0L, totalClicks);
        }
        if (ctr != null) {
            this.CTR = Math.max(0, ctr);
        }
    }

    public Long getUserId() {
        return this.user != null ? this.user.getId() : null;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
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

    public Long getTotalImpressions() {
        return totalImpressions;
    }

    public Long getTotalClicks() {
        return totalClicks;
    }

    public float getCTR() {
        return CTR;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}
