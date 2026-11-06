package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.NoticePlanningRepositoryPort;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Component
public class NoticePlanningJpaAdapter implements NoticePlanningRepositoryPort {
    static final RowMapper<Notification> NOTICE = (rs, i) -> new Notification(rs.getLong("id"), rs.getLong("recipient_id"),
            NotificationType.valueOf(rs.getString("notification_type")), Channel.valueOf(rs.getString("channel")), rs.getString("title"),
            rs.getString("message"), rs.getString("related_entity_id"), DeliveryStatus.valueOf(rs.getString("delivery_status")), LocalDateTime.ofInstant(rs.getTimestamp("sent_at").toInstant(), ZoneOffset.UTC));
    private static final RowMapper<Recipient> RECIPIENT = (rs, i) -> new Recipient(rs.getLong("user_id"), rs.getString("email"), rs.getBoolean("email_confirmed"), rs.getString("role"), rs.getObject("business_id", Long.class));
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    public NoticePlanningJpaAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; this.named = new NamedParameterJdbcTemplate(jdbc); }

    @Override
    public void lockRecipient(Long recipientId) { jdbc.queryForList("SELECT user_id FROM recipients WHERE user_id = ? FOR UPDATE", Long.class, recipientId); }
    @Override
    public Optional<Notification> caseNotice(Long recipientId, NotificationType type, String relatedId) { return jdbc.query("SELECT * FROM notifications WHERE recipient_id = ? AND notification_type = ? AND related_entity_id = ?", NOTICE, recipientId, type.name(), relatedId).stream().findFirst(); }
    @Override
    public List<Long> followers(Long businessId) { return jdbc.queryForList("SELECT consumer_id FROM followed_businesses WHERE business_id = ?", Long.class, businessId); }

    @Override
    public List<NearbyConsumer> nearby(OfferPublishedEvent offer, Instant since) {
        var sql = """
                SELECT consumer_id, ST_Distance(position, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography)::int AS meters
                FROM last_known_locations WHERE captured_at > :since
                AND ST_DWithin(position, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, radius_m)""";
        var parameters = new HashMap<String, Object>();
        parameters.put("lng", offer.location().longitude()); parameters.put("lat", offer.location().latitude()); parameters.put("since", Timestamp.from(since));
        if (offer.zone() != null) {
            sql += " AND ST_DWithin(position, ST_SetSRID(ST_MakePoint(:zoneLng, :zoneLat), 4326)::geography, :reach)";
            parameters.put("zoneLng", offer.zone().center().longitude()); parameters.put("zoneLat", offer.zone().center().latitude()); parameters.put("reach", offer.zone().reachMeters());
        }
        return named.query(sql, parameters, (rs, i) -> new NearbyConsumer(rs.getLong("consumer_id"), rs.getInt("meters")));
    }

    @Override
    public Optional<NotificationPreference> lockPreferences(Long consumerId) {
        return jdbc.query("SELECT * FROM notification_preferences WHERE consumer_id = ? FOR UPDATE", (rs, i) -> new NotificationPreference(rs.getLong("consumer_id"), rs.getBoolean("push_enabled"), rs.getBoolean("email_enabled"), rs.getInt("daily_limit"), rs.getTimestamp("updated_at").toLocalDateTime()), consumerId).stream().findFirst();
    }
    @Override
    public Optional<Recipient> recipient(Long userId) { return jdbc.query("SELECT * FROM recipients WHERE user_id = ?", RECIPIENT, userId).stream().findFirst(); }
    @Override
    public Optional<Recipient> businessOwner(Long businessId) { return jdbc.query("SELECT * FROM recipients WHERE business_id = ?", RECIPIENT, businessId).stream().findFirst(); }
    @Override
    public boolean hasPushSubscription(Long userId) { return jdbc.queryForObject("SELECT count(*) FROM push_subscriptions WHERE consumer_id = ?", Long.class, userId) > 0; }
    @Override
    public boolean hasFreshLocation(Long userId, Instant since) { return jdbc.queryForObject("SELECT count(*) FROM last_known_locations WHERE consumer_id = ? AND captured_at > ?", Long.class, userId, Timestamp.from(since)) > 0; }
    @Override
    public boolean existsOfferNotice(Long consumerId, Long offerId) { return jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id = ? AND related_entity_id = ? AND notification_type IN ('NEARBY_OFFER', 'FOLLOWED_BUSINESS_OFFER')", Long.class, consumerId, offerId.toString()) > 0; }
    @Override
    public long countOfferNoticesSince(Long consumerId, Instant start, Instant end) { return jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id = ? AND notification_type IN ('NEARBY_OFFER', 'FOLLOWED_BUSINESS_OFFER') AND delivery_status IN ('PENDING', 'SENT') AND sent_at >= ? AND sent_at < ?", Long.class, consumerId, Timestamp.from(start), Timestamp.from(end)); }
    @Override
    public Notification save(Notification notice) {
        return jdbc.queryForObject("""
                INSERT INTO notifications (recipient_id, notification_type, channel, title, message, related_entity_id, delivery_status, sent_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING *""", NOTICE, notice.getRecipientId(), notice.getType().name(), notice.getChannel().name(), notice.getTitle(), notice.getMessage(), notice.getRelatedEntityId(), notice.getStatus().name(), Timestamp.from(notice.getSentAt().toInstant(ZoneOffset.UTC)));
    }
    @Override
    public Optional<Notification> findNotice(Long id) { return jdbc.query("SELECT * FROM notifications WHERE id = ?", NOTICE, id).stream().findFirst(); }
    @Override
    public void status(Long noticeId, DeliveryStatus status) { jdbc.update("UPDATE notifications SET delivery_status = ? WHERE id = ?", status.name(), noticeId); }
    @Override
    public void fallbackToEmail(Long noticeId) { jdbc.update("UPDATE notifications SET channel = 'EMAIL', delivery_status = 'PENDING' WHERE id = ?", noticeId); }
    @Override
    public void cancelOffers(Collection<Long> ids) { ids.forEach(id -> jdbc.update("UPDATE notifications SET delivery_status = 'CANCELLED' WHERE related_entity_id = ? AND delivery_status = 'PENDING' AND notification_type IN ('NEARBY_OFFER', 'FOLLOWED_BUSINESS_OFFER')", id.toString())); }
    @Override
    public boolean existsCaseNotice(Long recipientId, NotificationType type, String relatedId) { return jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id = ? AND notification_type = ? AND related_entity_id = ?", Long.class, recipientId, type.name(), relatedId) > 0; }
}
