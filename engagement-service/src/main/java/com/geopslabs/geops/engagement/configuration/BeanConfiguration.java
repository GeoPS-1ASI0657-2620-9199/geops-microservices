package com.geopslabs.geops.engagement.configuration;

import com.geopslabs.geops.backend.identity.infrastructure.persistence.jpa.UserRepository;
import com.geopslabs.geops.backend.notifications.application.internal.outboundservices.NotificationFactoryService;
import com.geopslabs.geops.backend.offers.infrastructure.persistence.jpa.OfferRepository;
import com.geopslabs.geops.engagement.application.services.FavoriteCommandService;
import com.geopslabs.geops.engagement.application.services.FavoriteQueryService;
import com.geopslabs.geops.engagement.application.services.ReviewCommandService;
import com.geopslabs.geops.engagement.application.services.ReviewQueryService;
import com.geopslabs.geops.engagement.application.usecases.FavoriteCommandUseCase;
import com.geopslabs.geops.engagement.application.usecases.FavoriteQueryUseCase;
import com.geopslabs.geops.engagement.application.usecases.ReviewCommandUseCase;
import com.geopslabs.geops.engagement.application.usecases.ReviewQueryUseCase;
import com.geopslabs.geops.engagement.domain.ports.FavoriteRepositoryPort;
import com.geopslabs.geops.engagement.domain.ports.ReviewRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public FavoriteCommandUseCase favoriteCommandUseCase(FavoriteRepositoryPort favoriteRepository,
                                                         UserRepository userRepository,
                                                         OfferRepository offerRepository,
                                                         NotificationFactoryService notificationFactory) {
        return new FavoriteCommandService(favoriteRepository, userRepository, offerRepository, notificationFactory);
    }

    @Bean
    public FavoriteQueryUseCase favoriteQueryUseCase(FavoriteRepositoryPort favoriteRepository) {
        return new FavoriteQueryService(favoriteRepository);
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
