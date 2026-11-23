package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.domain.ports.ConfirmationEmailRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

@Component
public class ConfirmationEmailJdbcAdapter implements ConfirmationEmailRepositoryPort {
    private final JdbcTemplate jdbc;

    public ConfirmationEmailJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean claim(Long userId, Instant sentAt) {
        return jdbc.update("INSERT INTO confirmation_emails (user_id, sent_at) VALUES (?, ?) ON CONFLICT DO NOTHING",
                userId, Timestamp.from(sentAt)) == 1;
    }
}
