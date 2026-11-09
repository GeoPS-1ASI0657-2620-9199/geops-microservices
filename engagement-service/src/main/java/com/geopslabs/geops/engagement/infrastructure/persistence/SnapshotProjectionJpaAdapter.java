package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.application.exceptions.MissingLocalCopyException;
import com.geopslabs.geops.engagement.domain.models.*;
import com.geopslabs.geops.engagement.domain.ports.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;

@Component
public class SnapshotProjectionJpaAdapter implements SnapshotProjectionPort {
    private final JdbcTemplate jdbc;
    private final RedeemedReservationRepositoryPort redemptions;

    public SnapshotProjectionJpaAdapter(JdbcTemplate jdbc, RedeemedReservationRepositoryPort redemptions) {
        this.jdbc = jdbc;
        this.redemptions = redemptions;
    }

    @Override
    public void publish(BusinessSnapshot business, OfferSnapshot offer, Instant occurredAt) {
        jdbc.update("""
                INSERT INTO business_snapshots (business_id, business_name, updated_at) VALUES (?, ?, ?)
                ON CONFLICT (business_id) DO UPDATE SET business_name = EXCLUDED.business_name, updated_at = EXCLUDED.updated_at
                WHERE business_snapshots.updated_at <= EXCLUDED.updated_at""",
                business.businessId(), business.businessName(), Timestamp.from(occurredAt));
        jdbc.update("""
                INSERT INTO offer_snapshots (offer_id, business_id, title, valid_to, status, updated_at) VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (offer_id) DO UPDATE SET business_id = EXCLUDED.business_id, title = EXCLUDED.title,
                valid_to = EXCLUDED.valid_to, status = EXCLUDED.status, updated_at = EXCLUDED.updated_at
                WHERE offer_snapshots.updated_at <= EXCLUDED.updated_at""",
                offer.offerId(), offer.businessId(), offer.title(), offer.validTo(), offer.status(), Timestamp.from(occurredAt));
    }

    @Override
    public void changeStatus(Collection<Long> offerIds, String status, Instant occurredAt) {
        offerIds.forEach(id -> changeStatus(id, status, occurredAt));
    }

    private void changeStatus(Long offerId, String status, Instant occurredAt) {
        jdbc.update("UPDATE offer_snapshots SET status = ?, updated_at = ? WHERE offer_id = ? AND updated_at <= ?",
                status, Timestamp.from(occurredAt), offerId, Timestamp.from(occurredAt));
        if (jdbc.queryForObject("SELECT count(*) FROM offer_snapshots WHERE offer_id = ?", Long.class, offerId) == 0) {
            throw new MissingLocalCopyException("offer", offerId);
        }
    }

    @Override
    public void redeem(RedeemedReservation reservation) { redemptions.saveIfAbsent(reservation); }
}
