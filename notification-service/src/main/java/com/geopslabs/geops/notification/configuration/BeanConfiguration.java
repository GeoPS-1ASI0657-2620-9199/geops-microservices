package com.geopslabs.geops.notification.configuration;

import com.geopslabs.geops.notification.application.services.NotificationFactoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public NotificationFactoryService notificationFactoryService() {
        return new NotificationFactoryService();
    }
}
