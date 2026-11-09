package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.EmailDigestRepositoryPort;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Component
public class EmailDigestJpaAdapter implements EmailDigestRepositoryPort {
    private static final long SCHEDULER_LOCK = 9184262L;
    private final JdbcTemplate jdbc;
    public EmailDigestJpaAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override
    public void add(Long consumerId, Long offerId, Instant start, Instant end) {
        var updated = jdbc.update("""
                UPDATE email_digests SET offer_ids = CASE WHEN ? = ANY(offer_ids) THEN offer_ids ELSE array_append(offer_ids, ?) END
                WHERE consumer_id = ? AND window_start = ? AND sent_at IS NULL""", offerId, offerId, consumerId, Timestamp.from(start));
        if (updated == 0) jdbc.update("INSERT INTO email_digests (consumer_id, window_start, window_end, offer_ids) VALUES (?, ?, ?, ARRAY[?]::bigint[])", consumerId, Timestamp.from(start), Timestamp.from(end), offerId);
    }
    @Override
    public List<EmailDigest> due(Instant now) {
        return jdbc.query("SELECT * FROM email_digests WHERE window_end <= ? AND sent_at IS NULL ORDER BY id", (rs, i) -> new EmailDigest(rs.getLong("id"), rs.getLong("consumer_id"), rs.getTimestamp("window_end").toInstant(), Arrays.asList((Long[]) rs.getArray("offer_ids").getArray())), Timestamp.from(now));
    }
    @Override
    public List<Notification> notices(EmailDigest digest) {
        return digest.offerIds().stream().flatMap(id -> jdbc.query("SELECT * FROM notifications WHERE recipient_id = ? AND related_entity_id = ? AND channel = 'EMAIL' AND delivery_status = 'PENDING' AND notification_type IN ('NEARBY_OFFER', 'FOLLOWED_BUSINESS_OFFER') ORDER BY id", NoticePlanningJpaAdapter.NOTICE, digest.consumerId(), id.toString()).stream()).toList();
    }
    @Override
    public void complete(Long digestId, Instant now) { jdbc.update("UPDATE email_digests SET sent_at = ? WHERE id = ?", Timestamp.from(now), digestId); }
    @Override
    public void removeOffers(Collection<Long> offerIds) { offerIds.forEach(id -> jdbc.update("UPDATE email_digests SET offer_ids = array_remove(offer_ids, ?) WHERE sent_at IS NULL", id)); }
    @Override
    public void withSchedulerLock(Runnable action) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (var lock = connection.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
                lock.setLong(1, SCHEDULER_LOCK);
                try (var result = lock.executeQuery()) { result.next(); if (!result.getBoolean(1)) return null; }
                try { action.run(); }
                finally { try (var unlock = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) { unlock.setLong(1, SCHEDULER_LOCK); unlock.execute(); } }
            }
            return null;
        });
    }
}
