package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.SavedBusiness;
import com.geopslabs.geops.engagement.domain.ports.SavedBusinessRepositoryPort;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Component
public class SavedBusinessJpaAdapter implements SavedBusinessRepositoryPort {
    private static final RowMapper<SavedBusiness> ROW = (rs, i) -> new SavedBusiness(rs.getLong("id"), rs.getLong("consumer_id"), rs.getLong("business_id"), rs.getString("business_name"), rs.getTimestamp("saved_at").toInstant());
    private final JdbcTemplate jdbc;
    public SavedBusinessJpaAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<SavedBusiness> findByConsumer(Long consumerId) {
        return jdbc.query("SELECT s.*, b.business_name FROM saved_businesses s JOIN business_snapshots b USING (business_id) WHERE consumer_id = ? ORDER BY saved_at DESC, id DESC", ROW, consumerId);
    }

    @Override
    public java.util.Optional<SavedBusiness> saveIfAbsent(Long consumerId, Long businessId, Instant now) {
        return jdbc.query("""
                WITH inserted AS (
                    INSERT INTO saved_businesses (consumer_id, business_id, saved_at) VALUES (?, ?, ?)
                    ON CONFLICT (consumer_id, business_id) DO NOTHING RETURNING *)
                SELECT inserted.*, b.business_name FROM inserted JOIN business_snapshots b USING (business_id)""",
                ROW, consumerId, businessId, Timestamp.from(now)).stream().findFirst();
    }
    @Override
    public java.util.Optional<SavedBusiness> findByConsumerAndBusiness(Long consumerId, Long businessId) {
        return jdbc.query("SELECT s.*, b.business_name FROM saved_businesses s JOIN business_snapshots b USING (business_id) WHERE consumer_id = ? AND business_id = ?", ROW, consumerId, businessId).stream().findFirst();
    }

    @Override
    public long delete(Long consumerId, Long businessId) { return jdbc.update("DELETE FROM saved_businesses WHERE consumer_id = ? AND business_id = ?", consumerId, businessId); }
}
