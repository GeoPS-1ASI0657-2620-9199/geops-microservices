package com.geopslabs.geops.notification.configuration;

import com.geopslabs.geops.notification.application.services.*;
import com.geopslabs.geops.notification.domain.ports.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import java.time.Clock;

@Configuration
public class PreferencesConfiguration {
    @Bean
    NotificationPreferenceQueryService notificationPreferenceQueryService(NotificationPreferenceRepositoryPort preferences) { return new NotificationPreferenceQueryService(preferences); }
    @Bean
    RecipientProjectorService recipientProjectorService(RecipientRepositoryPort recipients, NotificationPreferenceRepositoryPort preferences, RecipientProjectionPort projections) { return new RecipientProjectorService(recipients, preferences, projections); }
    @Bean
    EmailUnsubscriptionService emailUnsubscriptionService(EmailUnsubscriptionRepositoryPort unsubscriptions, @Value("${geops.notifications.unsubscribe-secret}") String secret, Clock clock) { return new EmailUnsubscriptionService(unsubscriptions, secret, clock); }
}
