package com.geopslabs.geops.notification.configuration;

import com.geopslabs.geops.notification.application.services.*;
import com.geopslabs.geops.notification.application.ports.*;
import com.geopslabs.geops.notification.domain.ports.*;
import com.geopslabs.geops.notification.infrastructure.channels.SmtpEmailChannel;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.List;

@Configuration
@EnableScheduling
public class EmailConfiguration {
    public static final String SMTP = "smtp";
    @Bean
    SmtpEmailChannel smtpEmailChannel(JavaMailSender mail, @Value("${geops.notifications.email-from:avisos@geops.local}") String from,
                                      CircuitBreakerRegistry circuitBreakers, BulkheadRegistry bulkheads) {
        // US34: circuit breaker and bulkhead "smtp" from resilience4j.*.instances.smtp in application.yml.
        return new SmtpEmailChannel(mail, from, circuitBreakers.circuitBreaker(SMTP), bulkheads.bulkhead(SMTP));
    }
    @Bean
    EmailDigestService emailDigestService(EmailDigestRepositoryPort digests, @Value("${geops.notifications.email-window:60m}") String window) { return new EmailDigestService(digests, org.springframework.boot.convert.DurationStyle.detectAndParse(window)); }
    @Bean
    NotificationPlanner notificationPlanner(NoticePlanningRepositoryPort notices, EmailUnsubscriptionRepositoryPort unsubscriptions, EmailDigestRepositoryPort digestRepository, EmailDigestService digests, List<NotificationChannelStrategy> channels, PlatformTransactionManager manager, Clock clock) { return new NotificationPlanner(notices, unsubscriptions, digestRepository, digests, channels, new TransactionTemplate(manager), clock); }
    @Bean
    EmailDigestScheduler emailDigestScheduler(EmailDigestRepositoryPort digests, NoticePlanningRepositoryPort notices, EmailUnsubscriptionRepositoryPort unsubscriptions, EmailSender sender, PlatformTransactionManager manager, Clock clock, @Value("${geops.notifications.unsubscribe-secret}") String secret, @Value("${geops.notifications.web-base-url:http://localhost:4200}") String webBaseUrl) throws java.io.IOException {
        var template = new ClassPathResource("templates/email-digest.html").getContentAsString(StandardCharsets.UTF_8);
        return new EmailDigestScheduler(digests, notices, unsubscriptions, sender, new TransactionTemplate(manager), clock, secret, webBaseUrl, template);
    }
}
