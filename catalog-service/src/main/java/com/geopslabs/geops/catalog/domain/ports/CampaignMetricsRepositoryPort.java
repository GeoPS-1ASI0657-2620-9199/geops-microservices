package com.geopslabs.geops.catalog.domain.ports;

import com.geopslabs.geops.catalog.domain.models.DailyMetrics;
import com.geopslabs.geops.catalog.domain.models.MetricCounts;
import com.geopslabs.geops.catalog.domain.models.MetricsPeriod;
import com.geopslabs.geops.catalog.domain.models.OfferInteractions;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * campaign_daily_metrics. Every write adds to the row of the campaign of the offer and the day, creating it if
 * needed; offers without a campaign (public sources) count nothing.
 */
public interface CampaignMetricsRepositoryPort {
    void addInteractions(Collection<OfferInteractions> interactions);

    /** False when the offer has no campaign, so there is nothing to count. Runs in the caller's transaction. */
    boolean addReservation(Long offerId, LocalDate date);

    /** False when the offer has no campaign, so there is nothing to count. Runs in the caller's transaction. */
    boolean addRedemption(Long offerId, LocalDate date);

    /** Only the days with a row; the caller fills the rest with zeros. */
    List<DailyMetrics> findDaily(Long campaignId, MetricsPeriod period);

    /** Totals of the period per campaign of the business; campaigns without rows are not in the map. */
    Map<Long, MetricCounts> totalsByCampaign(Long businessId, MetricsPeriod period);
}
