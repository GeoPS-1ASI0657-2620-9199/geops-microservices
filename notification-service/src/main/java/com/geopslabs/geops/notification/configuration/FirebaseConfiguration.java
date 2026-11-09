package com.geopslabs.geops.notification.configuration;

import com.geopslabs.geops.notification.application.services.PushSubscriptionService;
import com.geopslabs.geops.notification.domain.ports.*;
import com.geopslabs.geops.notification.infrastructure.channels.FcmWebPushChannel;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.*;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import java.nio.file.*;
import java.time.Clock;

@Configuration
public class FirebaseConfiguration {
    private static final int TIMEOUT_MS = 2000;
    @Bean(destroyMethod = "delete")
    @Conditional(CredentialsConfigured.class)
    FirebaseApp firebaseApp(@Value("${geops.notifications.fcm-credentials-path}") String path) throws java.io.IOException {
        try (var stream = Files.newInputStream(Path.of(path))) {
            var options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(stream)).setConnectTimeout(TIMEOUT_MS).setReadTimeout(TIMEOUT_MS).build();
            return FirebaseApp.initializeApp(options, "geops-notification");
        }
    }
    @Bean
    @Conditional(CredentialsConfigured.class)
    FirebaseMessaging firebaseMessaging(FirebaseApp app) { return FirebaseMessaging.getInstance(app); }
    @Bean
    FcmWebPushChannel fcmWebPushChannel(ObjectProvider<FirebaseMessaging> messaging, PushSubscriptionRepositoryPort subscriptions, @Value("${geops.notifications.web-base-url:http://localhost:4200}") String url) { return new FcmWebPushChannel(messaging.getIfAvailable(), subscriptions, url); }
    @Bean
    PushSubscriptionService pushSubscriptionService(PushSubscriptionRepositoryPort subscriptions, RecipientRepositoryPort recipients, NotificationPreferenceRepositoryPort preferences, Clock clock) { return new PushSubscriptionService(subscriptions, recipients, preferences, clock); }
    static class CredentialsConfigured implements Condition {
        @Override public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) { return !context.getEnvironment().getProperty("geops.notifications.fcm-credentials-path", "").isBlank(); }
    }
}
