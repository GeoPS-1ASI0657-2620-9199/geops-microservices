package com.geopslabs.geops.catalog.configuration;

import com.geopslabs.geops.catalog.application.services.CampaignCommandService;
import com.geopslabs.geops.catalog.application.services.CampaignQueryService;
import com.geopslabs.geops.catalog.application.services.OfferQueryService;
import com.geopslabs.geops.catalog.application.usecases.CreateCampaignUseCase;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class BeanConfiguration {

    @Bean
    public Clock clock() {
        return Clock.system(OfferQueryService.OFFER_ZONE);
    }

    @Bean
    public OfferQueryService offerQueryService(OfferRepositoryPort offerRepository,
                                               MerchantStandingRepositoryPort merchantStandingRepository,
                                               Clock clock) {
        return new OfferQueryService(offerRepository, merchantStandingRepository, clock);
    }

    @Bean
    public CreateCampaignUseCase createCampaignUseCase(CampaignRepositoryPort campaignRepository,
                                                       OfferRepositoryPort offerRepository,
                                                       MerchantStandingRepositoryPort merchantStandingRepository,
                                                       Clock clock) {
        return new CampaignCommandService(campaignRepository, offerRepository, merchantStandingRepository, clock);
    }

    @Bean
    public CampaignQueryService campaignQueryService(CampaignRepositoryPort campaignRepository,
                                                     OfferRepositoryPort offerRepository) {
        return new CampaignQueryService(campaignRepository, offerRepository);
    }
}
