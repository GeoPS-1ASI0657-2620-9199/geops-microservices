package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.application.usecases.InboxNotificationView;
import com.geopslabs.geops.notification.domain.models.Recipient;
import com.geopslabs.geops.notification.domain.ports.RecipientRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import java.sql.Timestamp;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"geops.messaging.enabled=false", "geops.notifications.fcm-credentials-path="})
class NotificationInboxJdbcAdapterIntegrationTest {
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres")).withInitScript("acceptance/enable-postgis.sql");
    static { POSTGRES.start(); }
    private static final Instant BASE = Instant.parse("2026-11-23T10:00:00Z");
    @Autowired NotificationInboxJdbcAdapter adapter;
    @Autowired RecipientRepositoryPort recipients;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanAndSeedTwoUsers() {
        jdbc.execute("TRUNCATE TABLE notifications, recipients RESTART IDENTITY CASCADE");
        recipients.upsert(new Recipient(2001L, "uno@example.test", true, "CONSUMER"));
        recipients.upsert(new Recipient(2002L, "dos@example.test", true, "CONSUMER"));
    }

    private void insert(Long recipient, String type, String status, String title, String related, int minutes) {
        jdbc.update("INSERT INTO notifications (recipient_id, notification_type, channel, title, message, related_entity_id, delivery_status, sent_at) VALUES (?, ?, 'WEB_PUSH', ?, 'msg', ?, ?, ?)",
                recipient, type, title, related, status, Timestamp.from(BASE.plusSeconds(minutes * 60L)));
    }

    @Test
    void listsNewestFirstWithOfferIdOnlyForOfferNoticesAndSentAtAsInstant() {
        insert(2001L, "NEARBY_OFFER", "SENT", "vieja", "30001", 0);
        insert(2001L, "REPORT_RECEIVED", "PENDING", "reporte", "report-9", 10);
        insert(2001L, "FOLLOWED_BUSINESS_OFFER", "SENT", "nueva", "30002", 20);
        var list = adapter.findLatest(2001L, 20);
        assertThat(list).extracting(InboxNotificationView::title).containsExactly("nueva", "reporte", "vieja");
        assertThat(list).extracting(InboxNotificationView::offerId).containsExactly(30002L, null, 30001L);
        assertThat(list.get(0).createdAt()).isEqualTo(BASE.plusSeconds(1200));
        assertThat(list).extracting(InboxNotificationView::read).containsOnly(false);
        assertThat(adapter.findLatest(2001L, 2)).hasSize(2);
    }

    @Test
    void doesNotShowNoticesOfAnotherUserNorTheStatusesGeoPSDidNotSend() {
        insert(2001L, "NEARBY_OFFER", "SENT", "mia", "1", 0);
        insert(2002L, "NEARBY_OFFER", "SENT", "ajena", "2", 1);
        insert(2001L, "NEARBY_OFFER", "CANCELLED", "cancelada", "3", 2);
        insert(2001L, "NEARBY_OFFER", "SKIPPED_BY_LIMIT", "omitida", "4", 3);
        insert(2001L, "NEARBY_OFFER", "FAILED", "fallida", "5", 4);
        assertThat(adapter.findLatest(2001L, 20)).extracting(InboxNotificationView::title).containsExactly("mia");
        assertThat(adapter.countUnread(2001L)).isEqualTo(1L);
    }

    @Test
    void markAllReadOnlyTouchesTheCallerAndKeepsExcludedStatusesUntouched() {
        insert(2001L, "NEARBY_OFFER", "SENT", "mia", "1", 0);
        insert(2002L, "NEARBY_OFFER", "SENT", "ajena", "2", 1);
        insert(2001L, "NEARBY_OFFER", "CANCELLED", "cancelada", "3", 2);
        adapter.markAllRead(2001L, BASE.plusSeconds(3600));
        assertThat(adapter.countUnread(2001L)).isZero();
        assertThat(adapter.countUnread(2002L)).isEqualTo(1L);
        assertThat(adapter.findLatest(2001L, 20).get(0).read()).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id = 2001 AND read_at IS NULL", Long.class)).isEqualTo(1L);
        var firstRead = jdbc.queryForObject("SELECT read_at FROM notifications WHERE title = 'mia'", Timestamp.class);
        adapter.markAllRead(2001L, BASE.plusSeconds(7200));
        assertThat(jdbc.queryForObject("SELECT read_at FROM notifications WHERE title = 'mia'", Timestamp.class)).isEqualTo(firstRead);
    }
}
