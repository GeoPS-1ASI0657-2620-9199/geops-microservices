package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.BusinessBaselineUseCase;
import com.geopslabs.geops.catalog.application.usecases.DeclareBaselineCommand;
import com.geopslabs.geops.catalog.domain.models.BusinessBaseline;
import com.geopslabs.geops.catalog.domain.models.exceptions.BusinessNotFoundException;
import com.geopslabs.geops.catalog.domain.ports.BusinessBaselineRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Optional;

/**
 * The baseline of a business (US45). It hangs from the local copy of the business, so a business Catalog does not
 * know yet cannot declare one. Declaring again replaces the numbers and the date.
 */
public class BusinessBaselineService implements BusinessBaselineUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessBaselineService.class);

    private final BusinessBaselineRepositoryPort baselines;
    private final MerchantStandingRepositoryPort standings;
    private final Clock clock;

    public BusinessBaselineService(BusinessBaselineRepositoryPort baselines, MerchantStandingRepositoryPort standings,
                                   Clock clock) {
        this.baselines = baselines;
        this.standings = standings;
        this.clock = clock;
    }

    @Override
    public BusinessBaseline declare(DeclareBaselineCommand command) {
        var baseline = new BusinessBaseline(command.businessId(), command.customersPerWeek(), command.averageTicket(),
                clock.instant());
        if (standings.findByBusinessId(command.businessId()).isEmpty()) {
            throw new BusinessNotFoundException(command.businessId());
        }
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
