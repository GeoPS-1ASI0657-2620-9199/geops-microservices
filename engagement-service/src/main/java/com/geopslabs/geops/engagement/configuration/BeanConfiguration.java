package com.geopslabs.geops.engagement.configuration;

import com.geopslabs.geops.backend.identity.infrastructure.persistence.jpa.UserRepository;
import com.geopslabs.geops.backend.notifications.application.internal.outboundservices.NotificationFactoryService;
import com.geopslabs.geops.backend.offers.infrastructure.persistence.jpa.OfferRepository;
import com.geopslabs.geops.engagement.application.services.SavedOfferCommandService;
import com.geopslabs.geops.engagement.application.services.SavedOfferQueryService;
import com.geopslabs.geops.engagement.application.services.ReviewCommandService;
import com.geopslabs.geops.engagement.application.services.ReviewQueryService;
import com.geopslabs.geops.engagement.application.usecases.SavedOfferCommandUseCase;
import com.geopslabs.geops.engagement.application.usecases.SavedOfferQueryUseCase;
import com.geopslabs.geops.engagement.application.usecases.ReviewCommandUseCase;
import com.geopslabs.geops.engagement.application.usecases.ReviewQueryUseCase;
import com.geopslabs.geops.engagement.domain.ports.SavedOfferRepositoryPort;
import com.geopslabs.geops.engagement.domain.ports.ReviewRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public SavedOfferCommandUseCase savedOfferCommandUseCase(SavedOfferRepositoryPort savedOfferRepository,
                                                         UserRepository userRepository,
                                                         OfferRepository offerRepository,
                                                         NotificationFactoryService notificationFactory) {
        return new SavedOfferCommandService(savedOfferRepository, userRepository, offerRepository, notificationFactory);
    }

    @Bean
    public SavedOfferQueryUseCase savedOfferQueryUseCase(SavedOfferRepositoryPort savedOfferRepository) {
        return new SavedOfferQueryService(savedOfferRepository);
    }

    @Bean
    public ReviewCommandUseCase reviewCommandUseCase(ReviewRepositoryPort reviewRepository,
                                                     UserRepository userRepository,
                                                     OfferRepository offerRepository,
                                                     NotificationFactoryService notificationFactory) {
        return new ReviewCommandService(reviewRepository, userRepository, offerRepository, notificationFactory);
    }

    @Bean
    public ReviewQueryUseCase reviewQueryUseCase(ReviewRepositoryPort reviewRepository) {
        return new ReviewQueryService(reviewRepository);
    }
}
