package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.Follow;
import com.geopslabs.geops.engagement.domain.ports.FollowRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Component
public class FollowJpaAdapter implements FollowRepositoryPort {
    private static final RowMapper<Follow> ROW = (rs, index) -> new Follow(rs.getLong("id"),
            rs.getLong("consumer_id"), rs.getLong("business_id"), rs.getTimestamp("followed_at").toInstant());
    private final JdbcTemplate jdbc;

    public FollowJpaAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<Follow> findByConsumerAndBusiness(Long consumerId, Long businessId) {
        return jdbc.query("SELECT * FROM follows WHERE consumer_id = ? AND business_id = ?", ROW, consumerId, businessId).stream().findFirst();
    }

    @Override
    public Optional<Follow> saveIfAbsent(Follow follow) {
        return jdbc.query("""
                INSERT INTO follows (consumer_id, business_id, followed_at) VALUES (?, ?, ?)
                ON CONFLICT (consumer_id, business_id) DO NOTHING RETURNING *""", ROW,
                follow.consumerId(), follow.businessId(), Timestamp.from(follow.followedAt())).stream().findFirst();
    }

    @Override
    public long delete(Long consumerId, Long businessId) {
        return jdbc.update("DELETE FROM follows WHERE consumer_id = ? AND business_id = ?", consumerId, businessId);
    }

    @Override
    public List<Follow> findByConsumer(Long consumerId) {
        return jdbc.query("SELECT * FROM follows WHERE consumer_id = ? ORDER BY followed_at DESC, id DESC", ROW, consumerId);
    }
}
