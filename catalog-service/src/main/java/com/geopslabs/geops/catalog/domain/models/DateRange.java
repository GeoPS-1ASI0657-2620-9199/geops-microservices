package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidCampaignPeriodException;

import java.time.LocalDate;

public record DateRange(LocalDate start, LocalDate end) {

    public DateRange {
        if (end.isBefore(start)) {
            throw new InvalidCampaignPeriodException();
        }
    }

    public boolean hasEndedBefore(LocalDate today) {
        return end.isBefore(today);
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
}
