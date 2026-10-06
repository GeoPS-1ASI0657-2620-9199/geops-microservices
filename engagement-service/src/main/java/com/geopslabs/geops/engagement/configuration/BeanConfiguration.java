package com.geopslabs.geops.engagement.configuration;

import com.geopslabs.geops.engagement.application.services.ReviewCommandService;
import com.geopslabs.geops.engagement.application.services.ReviewQueryService;
import com.geopslabs.geops.engagement.application.services.SavedOfferCommandService;
import com.geopslabs.geops.engagement.application.services.SavedOfferQueryService;
import com.geopslabs.geops.engagement.application.usecases.CreateReviewUseCase;
import com.geopslabs.geops.engagement.application.usecases.ListBusinessReviewsUseCase;
import com.geopslabs.geops.engagement.domain.ports.BusinessSnapshotRepositoryPort;
import com.geopslabs.geops.engagement.domain.ports.OfferSnapshotRepositoryPort;
import com.geopslabs.geops.engagement.domain.ports.RedeemedReservationRepositoryPort;
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
    public SavedOfferCommandService savedOfferCommandService(SavedOfferRepositoryPort savedOfferRepository,
                                                             OfferSnapshotRepositoryPort offerSnapshotRepository,
                                                             BusinessSnapshotRepositoryPort businessSnapshotRepository,
                                                             Clock clock) {
        return new SavedOfferCommandService(savedOfferRepository, offerSnapshotRepository,
                businessSnapshotRepository, clock);
    }

    @Bean
    public SavedOfferQueryService savedOfferQueryService(SavedOfferRepositoryPort savedOfferRepository,
                                                         OfferSnapshotRepositoryPort offerSnapshotRepository,
                                                         BusinessSnapshotRepositoryPort businessSnapshotRepository,
                                                         Clock clock) {
        return new SavedOfferQueryService(savedOfferRepository, offerSnapshotRepository,
                businessSnapshotRepository, clock);
    }

    @Bean
    public CreateReviewUseCase createReviewUseCase(ReviewRepositoryPort reviewRepository,
                                                   RedeemedReservationRepositoryPort redeemedReservationRepository,
                                                   Clock clock) {
        return new ReviewCommandService(reviewRepository, redeemedReservationRepository, clock);
    }

    @Bean
    public ListBusinessReviewsUseCase listBusinessReviewsUseCase(ReviewRepositoryPort reviewRepository) {
        return new ReviewQueryService(reviewRepository);
    }
}
