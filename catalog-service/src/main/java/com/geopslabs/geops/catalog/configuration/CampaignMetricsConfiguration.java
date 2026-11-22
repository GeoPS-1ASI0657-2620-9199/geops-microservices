package com.geopslabs.geops.catalog.configuration;

import com.geopslabs.geops.catalog.application.services.BusinessBaselineService;
import com.geopslabs.geops.catalog.application.services.CampaignMetricsService;
import com.geopslabs.geops.catalog.application.services.OfferInteractionCounter;
import com.geopslabs.geops.catalog.application.usecases.GetCampaignByIdUseCase;
import com.geopslabs.geops.catalog.domain.ports.BusinessBaselineRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.CampaignMetricsRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.ProcessedEventsPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;

/** Módulo de Métricas: campaign metrics (US11) and baseline of the business (US45). */
@Configuration
public class CampaignMetricsConfiguration {

    @Bean
    public OfferInteractionCounter offerInteractionCounter(CampaignMetricsRepositoryPort metrics, Clock clock) {
        return new OfferInteractionCounter(metrics, clock);
    }

    @Bean
    @SuppressWarnings("java:S107")
    public CampaignMetricsService campaignMetricsService(CampaignMetricsRepositoryPort metrics,
                                                         ProcessedEventsPort processedEvents,
                                                         GetCampaignByIdUseCase campaignById,
                                                         CampaignRepositoryPort campaigns,
                                                         BusinessBaselineRepositoryPort baselines,
                                                         TransactionOperations transactions, Clock clock) {
        return new CampaignMetricsService(metrics, processedEvents, campaignById, campaigns, baselines, transactions,
                clock);
    }

    @Bean
    public BusinessBaselineService businessBaselineService(BusinessBaselineRepositoryPort baselines,
                                                           MerchantStandingRepositoryPort standings, Clock clock) {
        return new BusinessBaselineService(baselines, standings, clock);
    }
}
