package com.geopslabs.geops.identity.configuration;

import com.geopslabs.geops.backend.notifications.application.internal.outboundservices.NotificationFactoryService;
import com.geopslabs.geops.identity.application.services.DetailsConsumerCommandService;
import com.geopslabs.geops.identity.application.services.DetailsConsumerQueryService;
import com.geopslabs.geops.identity.application.services.DetailsOwnerCommandService;
import com.geopslabs.geops.identity.application.services.DetailsOwnerQueryService;
import com.geopslabs.geops.identity.application.services.UserCommandService;
import com.geopslabs.geops.identity.application.services.UserQueryService;
import com.geopslabs.geops.identity.application.usecases.DetailsConsumerCommandUseCase;
import com.geopslabs.geops.identity.application.usecases.DetailsConsumerQueryUseCase;
import com.geopslabs.geops.identity.application.usecases.DetailsOwnerCommandUseCase;
import com.geopslabs.geops.identity.application.usecases.DetailsOwnerQueryUseCase;
import com.geopslabs.geops.identity.application.usecases.UserCommandUseCase;
import com.geopslabs.geops.identity.application.usecases.UserQueryUseCase;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.ConsumerProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.PasswordHasherPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public UserCommandUseCase userCommandUseCase(UserRepositoryPort userRepository,
                                                 NotificationFactoryService notificationFactory,
                                                 PasswordHasherPort passwordHasher) {
        return new UserCommandService(userRepository, notificationFactory, passwordHasher);
    }

    @Bean
    public UserQueryUseCase userQueryUseCase(UserRepositoryPort userRepository) {
        return new UserQueryService(userRepository);
    }

    @Bean
    public DetailsConsumerCommandUseCase detailsConsumerCommandUseCase(
            ConsumerProfileRepositoryPort consumerProfileRepository, UserRepositoryPort userRepository) {
        return new DetailsConsumerCommandService(consumerProfileRepository, userRepository);
    }

    @Bean
    public DetailsConsumerQueryUseCase detailsConsumerQueryUseCase(
            ConsumerProfileRepositoryPort consumerProfileRepository) {
        return new DetailsConsumerQueryService(consumerProfileRepository);
    }

    @Bean
    public DetailsOwnerCommandUseCase detailsOwnerCommandUseCase(
            BusinessProfileRepositoryPort businessProfileRepository, UserRepositoryPort userRepository) {
        return new DetailsOwnerCommandService(businessProfileRepository, userRepository);
    }

    @Bean
    public DetailsOwnerQueryUseCase detailsOwnerQueryUseCase(BusinessProfileRepositoryPort businessProfileRepository) {
        return new DetailsOwnerQueryService(businessProfileRepository);
    }
}
