package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.domain.ports.RecipientProjectionPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RecipientProjectionJpaAdapter implements RecipientProjectionPort {
    private final JdbcTemplate jdbc;
    public RecipientProjectionJpaAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override
    public void updateRadius(Long consumerId, int radiusMeters) { jdbc.update("UPDATE last_known_locations SET radius_m = ? WHERE consumer_id = ?", radiusMeters, consumerId); }
    @Override
    public void follow(Long consumerId, Long businessId) { jdbc.update("INSERT INTO followed_businesses (consumer_id, business_id) VALUES (?, ?) ON CONFLICT DO NOTHING", consumerId, businessId); }
    @Override
    public void unfollow(Long consumerId, Long businessId) { jdbc.update("DELETE FROM followed_businesses WHERE consumer_id = ? AND business_id = ?", consumerId, businessId); }
}
