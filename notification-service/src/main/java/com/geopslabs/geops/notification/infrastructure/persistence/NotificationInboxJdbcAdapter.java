package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.application.ports.NotificationInboxRepositoryPort;
import com.geopslabs.geops.notification.application.usecases.InboxNotificationView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Component
public class NotificationInboxJdbcAdapter implements NotificationInboxRepositoryPort {
    private static final Set<String> OFFER_TYPES = Set.of("NEARBY_OFFER", "FOLLOWED_BUSINESS_OFFER");
    private static final String VISIBLE = "delivery_status IN ('SENT', 'PENDING')";
    private static final RowMapper<InboxNotificationView> VIEW = (rs, i) -> {
        var type = rs.getString("notification_type");
        return new InboxNotificationView(rs.getLong("id"), type, rs.getString("title"), rs.getString("message"),
                offerId(type, rs.getString("related_entity_id")), rs.getTimestamp("sent_at").toInstant(), rs.getTimestamp("read_at") != null);
    };
    private final JdbcTemplate jdbc;
    public NotificationInboxJdbcAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<InboxNotificationView> findLatest(Long recipientId, int size) {
        return jdbc.query("SELECT id, notification_type, title, message, related_entity_id, sent_at, read_at FROM notifications WHERE recipient_id = ? AND " + VISIBLE + " ORDER BY sent_at DESC, id DESC LIMIT ?", VIEW, recipientId, size);
    }
    @Override
    public long countUnread(Long recipientId) { return jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id = ? AND read_at IS NULL AND " + VISIBLE, Long.class, recipientId); }
    @Override
    public void markAllRead(Long recipientId, Instant readAt) { jdbc.update("UPDATE notifications SET read_at = ? WHERE recipient_id = ? AND read_at IS NULL AND " + VISIBLE, Timestamp.from(readAt), recipientId); }

    private static Long offerId(String type, String relatedEntityId) {
        if (!OFFER_TYPES.contains(type) || relatedEntityId == null) return null;
        try { return Long.valueOf(relatedEntityId); } catch (NumberFormatException exception) { return null; }
    }
}
