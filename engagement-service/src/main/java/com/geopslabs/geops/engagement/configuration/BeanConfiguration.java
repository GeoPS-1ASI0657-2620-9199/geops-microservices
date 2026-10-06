package com.geopslabs.geops.engagement.configuration;

import com.geopslabs.geops.engagement.application.services.ReviewCommandService;
import com.geopslabs.geops.engagement.application.services.ReviewQueryService;
import com.geopslabs.geops.engagement.application.services.SavedOfferCommandService;
import com.geopslabs.geops.engagement.application.services.SavedOfferQueryService;
import com.geopslabs.geops.engagement.application.usecases.ReviewCommandUseCase;
import com.geopslabs.geops.engagement.application.usecases.ReviewQueryUseCase;
import com.geopslabs.geops.engagement.application.usecases.SavedOfferCommandUseCase;
import com.geopslabs.geops.engagement.application.usecases.SavedOfferQueryUseCase;
import com.geopslabs.geops.engagement.domain.ports.ReviewRepositoryPort;
import com.geopslabs.geops.engagement.domain.ports.SavedOfferRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class BeanConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public SavedOfferCommandUseCase savedOfferCommandUseCase(SavedOfferRepositoryPort savedOfferRepository) {
        return new SavedOfferCommandService(savedOfferRepository);
    }

    @Bean
    public SavedOfferQueryUseCase savedOfferQueryUseCase(SavedOfferRepositoryPort savedOfferRepository) {
        return new SavedOfferQueryService(savedOfferRepository);
    }

    @Bean
    public ReviewCommandUseCase reviewCommandUseCase(ReviewRepositoryPort reviewRepository) {
        return new ReviewCommandService(reviewRepository);
    }

    @Bean
    public ReviewQueryUseCase reviewQueryUseCase(ReviewRepositoryPort reviewRepository) {
        return new ReviewQueryService(reviewRepository);
    }
}
