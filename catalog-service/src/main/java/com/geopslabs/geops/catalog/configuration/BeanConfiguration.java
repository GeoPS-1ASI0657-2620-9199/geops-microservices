package com.geopslabs.geops.catalog.configuration;

import com.geopslabs.geops.catalog.application.services.CampaignQueryService;
import com.geopslabs.geops.catalog.application.services.OfferQueryService;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public OfferQueryService offerQueryService(OfferRepositoryPort offerRepository) {
        return new OfferQueryService(offerRepository);
    }

    @Bean
    public CampaignQueryService campaignQueryService(CampaignRepositoryPort campaignRepository,
                                                     OfferRepositoryPort offerRepository) {
        return new CampaignQueryService(campaignRepository, offerRepository);
    }
}
