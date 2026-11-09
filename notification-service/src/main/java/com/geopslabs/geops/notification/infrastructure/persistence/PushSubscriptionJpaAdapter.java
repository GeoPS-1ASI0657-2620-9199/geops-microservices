package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.domain.models.PushSubscription;
import com.geopslabs.geops.notification.domain.ports.PushSubscriptionRepositoryPort;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;
import java.util.*;

@Component
public class PushSubscriptionJpaAdapter implements PushSubscriptionRepositoryPort {
    private static final RowMapper<PushSubscription> ROW = (rs, i) -> new PushSubscription(rs.getLong("id"), rs.getLong("consumer_id"), rs.getString("fcm_token"), rs.getString("user_agent"), rs.getTimestamp("created_at").toInstant());
    private final JdbcTemplate jdbc;
    public PushSubscriptionJpaAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override
    public Optional<PushSubscription> saveIfAbsent(PushSubscription subscription) {
        return jdbc.query("INSERT INTO push_subscriptions (consumer_id, fcm_token, user_agent, created_at) VALUES (?, ?, ?, ?) ON CONFLICT (fcm_token) DO NOTHING RETURNING *", ROW, subscription.consumerId(), subscription.fcmToken(), subscription.userAgent(), Timestamp.from(subscription.createdAt())).stream().findFirst();
    }
    @Override
    public Optional<PushSubscription> findByToken(String token) { return jdbc.query("SELECT * FROM push_subscriptions WHERE fcm_token = ?", ROW, token).stream().findFirst(); }
    @Override
    public List<PushSubscription> findByConsumer(Long consumerId) { return jdbc.query("SELECT * FROM push_subscriptions WHERE consumer_id = ? ORDER BY id", ROW, consumerId); }
    @Override
    public void delete(Long id, Long consumerId) { jdbc.update("DELETE FROM push_subscriptions WHERE id = ? AND consumer_id = ?", id, consumerId); }
}
