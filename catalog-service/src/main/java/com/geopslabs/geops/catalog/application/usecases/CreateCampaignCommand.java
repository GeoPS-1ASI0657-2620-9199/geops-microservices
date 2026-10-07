package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.ZoneType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateCampaignCommand(
        Long businessId,
        String businessName,
        String name,
        String description,
        LocalDate start,
        LocalDate end,
        BigDecimal estimatedBudget,
        String storeAddress,
        GeoPoint storeLocation,
        ZoneType zoneType,
        GeoPoint zoneCenter,
        Integer radiusMeters,
        String district,
        List<CreateOfferCommand> offers) {
}
