package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.DailyMetrics;
import com.geopslabs.geops.catalog.domain.models.MetricCounts;
import com.geopslabs.geops.catalog.domain.models.MetricsPeriod;
import com.geopslabs.geops.catalog.domain.models.OfferInteractions;
import com.geopslabs.geops.catalog.domain.ports.CampaignMetricsRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * campaign_daily_metrics with plain SQL. Each write is one upsert that finds the campaign of the offer in the same
 * statement and adds to the row of the day, so concurrent writers never lose a count and an offer without campaign
 * (a public source) matches no row and counts nothing. Impressions and clicks go in one JDBC batch per flush.
 */
@Component
public class CampaignMetricsJdbcAdapter implements CampaignMetricsRepositoryPort {
    private static final String ADD_INTERACTIONS = """
            INSERT INTO campaign_daily_metrics (campaign_id, metric_date, impressions, clicks)
            SELECT o.campaign_id, ?, ?, ? FROM offers o WHERE o.id = ? AND o.campaign_id IS NOT NULL
            ON CONFLICT (campaign_id, metric_date) DO UPDATE
            SET impressions = campaign_daily_metrics.impressions + EXCLUDED.impressions,
                clicks = campaign_daily_metrics.clicks + EXCLUDED.clicks
            """;
    private static final String ADD_RESERVATION = """
            INSERT INTO campaign_daily_metrics (campaign_id, metric_date, reservations)
            SELECT o.campaign_id, ?, 1 FROM offers o WHERE o.id = ? AND o.campaign_id IS NOT NULL
            ON CONFLICT (campaign_id, metric_date) DO UPDATE
            SET reservations = campaign_daily_metrics.reservations + 1
            """;
    private static final String ADD_REDEMPTION = """
            INSERT INTO campaign_daily_metrics (campaign_id, metric_date, conversions)
            SELECT o.campaign_id, ?, 1 FROM offers o WHERE o.id = ? AND o.campaign_id IS NOT NULL
            ON CONFLICT (campaign_id, metric_date) DO UPDATE
            SET conversions = campaign_daily_metrics.conversions + 1
            """;
    private static final String FIND_DAILY = """
            SELECT metric_date, impressions, clicks, reservations, conversions
            FROM campaign_daily_metrics
            WHERE campaign_id = ? AND metric_date BETWEEN ? AND ?
            ORDER BY metric_date
            """;
    private static final String TOTALS_BY_CAMPAIGN = """
            SELECT m.campaign_id, SUM(m.impressions) AS impressions, SUM(m.clicks) AS clicks,
                   SUM(m.reservations) AS reservations, SUM(m.conversions) AS conversions
            FROM campaign_daily_metrics m JOIN campaigns c ON c.id = m.campaign_id
            WHERE c.business_id = ? AND m.metric_date BETWEEN ? AND ?
            GROUP BY m.campaign_id
            """;

    private final JdbcTemplate jdbc;

    public CampaignMetricsJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void addInteractions(Collection<OfferInteractions> interactions) {
        var rows = new ArrayList<Object[]>(interactions.size());
        for (var counted : interactions) {
            rows.add(new Object[]{Date.valueOf(counted.date()), counted.impressions(), counted.clicks(),
                    counted.offerId()});
        }
        jdbc.batchUpdate(ADD_INTERACTIONS, rows);
    }

    @Override
    public boolean addReservation(Long offerId, LocalDate date) {
        return jdbc.update(ADD_RESERVATION, Date.valueOf(date), offerId) == 1;
    }

    @Override
    public boolean addRedemption(Long offerId, LocalDate date) {
        return jdbc.update(ADD_REDEMPTION, Date.valueOf(date), offerId) == 1;
    }

    @Override
    public List<DailyMetrics> findDaily(Long campaignId, MetricsPeriod period) {
        return jdbc.query(FIND_DAILY, (row, index) -> new DailyMetrics(row.getDate("metric_date").toLocalDate(),
                countsOf(row)), campaignId, Date.valueOf(period.from()), Date.valueOf(period.to()));
    }

    @Override
    public Map<Long, MetricCounts> totalsByCampaign(Long businessId, MetricsPeriod period) {
        var totals = new HashMap<Long, MetricCounts>();
        jdbc.query(TOTALS_BY_CAMPAIGN, row -> {
            totals.put(row.getLong("campaign_id"), countsOf(row));
        }, businessId, Date.valueOf(period.from()), Date.valueOf(period.to()));
        return totals;
    }

    private static MetricCounts countsOf(ResultSet row) throws SQLException {
        return new MetricCounts(row.getLong("impressions"), row.getLong("clicks"), row.getLong("reservations"),
                row.getLong("conversions"));
    }
}
