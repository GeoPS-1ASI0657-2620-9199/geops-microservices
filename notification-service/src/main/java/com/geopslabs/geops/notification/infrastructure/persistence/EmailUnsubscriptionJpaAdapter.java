package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.EmailUnsubscriptionRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;

@Component
public class EmailUnsubscriptionJpaAdapter implements EmailUnsubscriptionRepositoryPort {
    private final JdbcTemplate jdbc;
    public EmailUnsubscriptionJpaAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override
    public EmailUnsubscription saveIfAbsent(EmailUnsubscription value) {
        jdbc.update("INSERT INTO email_unsubscriptions (consumer_id, notification_type, token, unsubscribed_at) VALUES (?, ?, ?, ?) ON CONFLICT (token) DO NOTHING", value.consumerId(), value.notificationType().name(), value.token(), Timestamp.from(value.unsubscribedAt()));
        return jdbc.queryForObject("SELECT * FROM email_unsubscriptions WHERE token = ?", (rs, i) -> new EmailUnsubscription(rs.getLong("consumer_id"), NotificationType.valueOf(rs.getString("notification_type")), rs.getString("token"), rs.getTimestamp("unsubscribed_at").toInstant()), value.token());
    }
    @Override
    public boolean exists(Long consumerId, NotificationType type) { return jdbc.queryForObject("SELECT count(*) FROM email_unsubscriptions WHERE consumer_id = ? AND notification_type = ?", Long.class, consumerId, type.name()) > 0; }
}
