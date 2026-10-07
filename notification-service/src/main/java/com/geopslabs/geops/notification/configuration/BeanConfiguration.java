package com.geopslabs.geops.notification.configuration;

import com.geopslabs.geops.notification.application.services.LastKnownLocationCommandService;
import com.geopslabs.geops.notification.application.services.NotificationFactoryService;
import com.geopslabs.geops.notification.application.services.NotificationPreferenceCommandService;
import com.geopslabs.geops.notification.application.usecases.RecordLastKnownLocationUseCase;
import com.geopslabs.geops.notification.application.usecases.UpdateNotificationPreferencesUseCase;
import com.geopslabs.geops.notification.domain.ports.LastKnownLocationRepositoryPort;
import com.geopslabs.geops.notification.domain.ports.NotificationPreferenceRepositoryPort;
import com.geopslabs.geops.notification.domain.ports.RecipientRepositoryPort;
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
    public NotificationFactoryService notificationFactoryService() {
        return new NotificationFactoryService();
    }

    @Bean
    public UpdateNotificationPreferencesUseCase updateNotificationPreferencesUseCase(
            RecipientRepositoryPort recipientRepository, NotificationPreferenceRepositoryPort preferenceRepository,
            Clock clock) {
        return new NotificationPreferenceCommandService(recipientRepository, preferenceRepository, clock);
    }

    @Bean
    public RecordLastKnownLocationUseCase recordLastKnownLocationUseCase(
            RecipientRepositoryPort recipientRepository, NotificationPreferenceRepositoryPort preferenceRepository,
            LastKnownLocationRepositoryPort locationRepository, Clock clock) {
        return new LastKnownLocationCommandService(recipientRepository, preferenceRepository, locationRepository,
                clock);
    }
}
