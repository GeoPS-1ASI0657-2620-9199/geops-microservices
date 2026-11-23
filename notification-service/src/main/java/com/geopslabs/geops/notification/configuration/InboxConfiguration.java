package com.geopslabs.geops.notification.configuration;

import com.geopslabs.geops.notification.application.ports.NotificationInboxRepositoryPort;
import com.geopslabs.geops.notification.application.services.NotificationInboxService;
import org.springframework.context.annotation.*;
import java.time.Clock;

@Configuration
public class InboxConfiguration {
    @Bean
    NotificationInboxService notificationInboxService(NotificationInboxRepositoryPort inbox, Clock clock) { return new NotificationInboxService(inbox, clock); }
}
