package com.geopslabs.geops.catalog.configuration;

import com.geopslabs.geops.catalog.application.services.CampaignCommandService;
import com.geopslabs.geops.catalog.application.services.CampaignQueryService;
import com.geopslabs.geops.catalog.application.services.OfferCommandService;
import com.geopslabs.geops.catalog.application.services.OfferQueryService;
import com.geopslabs.geops.catalog.application.usecases.CampaignCommandUseCase;
import com.geopslabs.geops.catalog.application.usecases.CampaignQueryUseCase;
import com.geopslabs.geops.catalog.application.usecases.OfferCommandUseCase;
import com.geopslabs.geops.catalog.application.usecases.OfferQueryUseCase;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public OfferCommandUseCase offerCommandUseCase(OfferRepositoryPort offerRepository,
                                                   CampaignRepositoryPort campaignRepository) {
        return new OfferCommandService(offerRepository, campaignRepository);
    }

    @Bean
    public OfferQueryUseCase offerQueryUseCase(OfferRepositoryPort offerRepository,
                                               CampaignRepositoryPort campaignRepository) {
        return new OfferQueryService(offerRepository, campaignRepository);
    }

    @Bean
    public CampaignCommandUseCase campaignCommandUseCase(CampaignRepositoryPort campaignRepository) {
        return new CampaignCommandService(campaignRepository);
    }

    @Bean
    public CampaignQueryUseCase campaignQueryUseCase(CampaignRepositoryPort campaignRepository) {
        return new CampaignQueryService(campaignRepository);
    }
}
