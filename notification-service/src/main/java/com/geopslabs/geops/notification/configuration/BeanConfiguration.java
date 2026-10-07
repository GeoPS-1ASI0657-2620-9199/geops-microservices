package com.geopslabs.geops.notification.configuration;

import com.geopslabs.geops.notification.application.services.NotificationCommandService;
import com.geopslabs.geops.notification.application.services.NotificationFactoryService;
import com.geopslabs.geops.notification.application.services.NotificationQueryService;
import com.geopslabs.geops.notification.application.usecases.NotificationCommandUseCase;
import com.geopslabs.geops.notification.application.usecases.NotificationQueryUseCase;
import com.geopslabs.geops.notification.domain.ports.NotificationRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public NotificationCommandUseCase notificationCommandUseCase(NotificationRepositoryPort notificationRepository) {
        return new NotificationCommandService(notificationRepository);
    }

    @Bean
    public NotificationQueryUseCase notificationQueryUseCase(NotificationRepositoryPort notificationRepository) {
        return new NotificationQueryService(notificationRepository);
    }

    @Bean
    public NotificationFactoryService notificationFactoryService(NotificationCommandUseCase notificationCommandService) {
        return new NotificationFactoryService(notificationCommandService);
    }
}
