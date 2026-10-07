package com.geopslabs.geops.catalog.configuration;

import com.geopslabs.geops.catalog.application.services.NearbyOfferQueryService;
import com.geopslabs.geops.catalog.application.usecases.SearchNearbyOffersUseCase;
import com.geopslabs.geops.catalog.domain.models.ProximitySearchService;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ProximitySearchConfiguration {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    @Bean
    public ProximitySearchService proximitySearchService(OfferRepositoryPort offerRepository) {
        return new ProximitySearchService(offerRepository, Clock.system(LIMA));
    }

    @Bean
    public SearchNearbyOffersUseCase searchNearbyOffersUseCase(ProximitySearchService proximitySearchService) {
        return new NearbyOfferQueryService(proximitySearchService);
    }
}
