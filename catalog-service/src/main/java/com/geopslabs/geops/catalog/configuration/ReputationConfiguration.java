package com.geopslabs.geops.catalog.configuration;

import com.geopslabs.geops.catalog.application.services.CellInvalidator;
import com.geopslabs.geops.catalog.application.services.MerchantComplianceService;
import com.geopslabs.geops.catalog.application.usecases.ApplyComplianceEventUseCase;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.ReputationEventsPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;

@Configuration
public class ReputationConfiguration {

    @Bean
    public ApplyComplianceEventUseCase applyComplianceEventUseCase(MerchantStandingRepositoryPort standings,
                                                                   ReputationEventsPort reputationEvents,
                                                                   CellInvalidator cellInvalidator,
                                                                   TransactionOperations transactions,
                                                                   Clock clock) {
        return new MerchantComplianceService(standings, reputationEvents, cellInvalidator, transactions, clock);
    }
}
