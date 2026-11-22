package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.BusinessBaselineUseCase;
import com.geopslabs.geops.catalog.application.usecases.DeclareBaselineCommand;
import com.geopslabs.geops.catalog.domain.models.BusinessBaseline;
import com.geopslabs.geops.catalog.domain.ports.BusinessBaselineRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Optional;

/**
 * The baseline of a business (US45). Any owner with a businessId in the token can declare it, even before Catalog
 * receives the local copy of the business: a business that just registered declares it before its first campaign.
 * Declaring again replaces the numbers and the date.
 */
public class BusinessBaselineService implements BusinessBaselineUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessBaselineService.class);

    private final BusinessBaselineRepositoryPort baselines;
    private final Clock clock;

    public BusinessBaselineService(BusinessBaselineRepositoryPort baselines, Clock clock) {
        this.baselines = baselines;
        this.clock = clock;
    }

    @Override
    public BusinessBaseline declare(DeclareBaselineCommand command) {
        var baseline = new BusinessBaseline(command.businessId(), command.customersPerWeek(), command.averageTicket(),
                clock.instant());
        var saved = baselines.save(baseline);
        LOGGER.info("business.baseline.declared businessId={} customersPerWeek={}", saved.businessId(),
                saved.customersPerWeek());
        return saved;
    }

    @Override
    public Optional<BusinessBaseline> find(Long businessId) {
        return baselines.findByBusinessId(businessId);
    }
}
