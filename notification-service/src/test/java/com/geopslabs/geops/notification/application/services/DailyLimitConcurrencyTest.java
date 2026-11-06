package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "geops.messaging.enabled=false")
class DailyLimitConcurrencyTest {
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres")).withInitScript("acceptance/enable-postgis.sql");
    static { POSTGRES.start(); }
    @Autowired NotificationPlanner planner;
    @Autowired RecipientRepositoryPort recipients;
    @Autowired NotificationPreferenceRepositoryPort preferences;
    @Autowired RecipientProjectionPort projections;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
    @Autowired NoticePlanningRepositoryPort notices;
    @Autowired EmailDigestRepositoryPort digests;
    @Autowired EmailUnsubscriptionRepositoryPort unsubscriptions;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Test
    void tenSimultaneousOffersReserveOnlyThreeDailySlots() throws Exception {
        recipients.upsert(new Recipient(2001L, "consumer@example.test", true, "CONSUMER"));
        preferences.createIfAbsent(new NotificationPreference(2001L, false, true, 3, LocalDateTime.now(clock)));
        projections.follow(2001L, 84L);
        var executor = Executors.newFixedThreadPool(10); var start = new CountDownLatch(1); var futures = new ArrayList<Future<?>>();
        try {
            for (long id = 1000; id < 1010; id++) {
                var offerId = id;
                futures.add(executor.submit(() -> { start.await(); planner.offerPublished(new OfferPublishedEvent(offerId, 84L, "Don Pepe", "2x1", LocalDate.now(clock).plusDays(2), new GeoPoint(-12.1, -77.0), null, clock.instant())); return null; }));
            }
            start.countDown();
            for (var future : futures) future.get(30, TimeUnit.SECONDS);
        } finally { executor.shutdownNow(); }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE delivery_status = 'PENDING'", Long.class)).isEqualTo(3L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE delivery_status = 'SKIPPED_BY_LIMIT'", Long.class)).isEqualTo(7L);
        preferences.upsert(new NotificationPreference(2001L, false, true, 1, LocalDateTime.now(clock)));
        var sender = org.mockito.Mockito.mock(com.geopslabs.geops.notification.application.ports.EmailSender.class);
        new EmailDigestScheduler(digests, notices, unsubscriptions, sender,
                new org.springframework.transaction.support.TransactionTemplate(transactions),
                Clock.offset(clock, Duration.ofHours(2)), "test-key", "http://localhost:4200", "<ul>{{offers}}</ul>{{unsubscribeLinks}}").sendDue();
        org.mockito.Mockito.verify(sender).send(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.argThat(html -> html.split("<li>").length - 1 == 1));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE delivery_status = 'SENT'", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE delivery_status = 'SKIPPED_BY_LIMIT'", Long.class)).isEqualTo(9L);
    }
}
