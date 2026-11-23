package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.ApplyReservationMetricEventUseCase;
import com.geopslabs.geops.catalog.application.usecases.CampaignMetrics;
import com.geopslabs.geops.catalog.application.usecases.CampaignMetricsSummary;
import com.geopslabs.geops.catalog.application.usecases.GetCampaignByIdUseCase;
import com.geopslabs.geops.catalog.application.usecases.GetCampaignMetricsUseCase;
import com.geopslabs.geops.catalog.application.usecases.ListCampaignMetricsUseCase;
import com.geopslabs.geops.catalog.application.usecases.ReservationMetricEvent;
import com.geopslabs.geops.catalog.domain.models.DailyMetrics;
import com.geopslabs.geops.catalog.domain.models.MetricCounts;
import com.geopslabs.geops.catalog.domain.models.MetricsPeriod;
import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignByIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignMetricsQuery;
import com.geopslabs.geops.catalog.domain.models.queries.ListCampaignMetricsQuery;
import com.geopslabs.geops.catalog.domain.ports.BusinessBaselineRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.CampaignMetricsRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.ProcessedEventsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.LocalDate;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Módulo de Métricas (US11, US45). Reservations and redemptions come from reservation.events, idempotent by eventId
 * with its own row in processed_events; impressions and clicks come from {@link OfferInteractionCounter}. The
 * owner reads one campaign by day or every campaign with its totals; the access rule is the one of
 * GetCampaignById.
 */
public class CampaignMetricsService
        implements ApplyReservationMetricEventUseCase, GetCampaignMetricsUseCase, ListCampaignMetricsUseCase {
    public static final String CONSUMER = "campaign-metrics";
    static final int SUMMARY_DAYS = 30;
    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignMetricsService.class);

    private final CampaignMetricsRepositoryPort metrics;
    private final ProcessedEventsPort processedEvents;
    private final GetCampaignByIdUseCase campaignById;
    private final CampaignRepositoryPort campaigns;
    private final BusinessBaselineRepositoryPort baselines;
    private final TransactionOperations transactions;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public CampaignMetricsService(CampaignMetricsRepositoryPort metrics, ProcessedEventsPort processedEvents,
                                  GetCampaignByIdUseCase campaignById, CampaignRepositoryPort campaigns,
                                  BusinessBaselineRepositoryPort baselines, TransactionOperations transactions,
                                  Clock clock) {
        this.metrics = metrics;
        this.processedEvents = processedEvents;
        this.campaignById = campaignById;
        this.campaigns = campaigns;
        this.baselines = baselines;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Outcome apply(ReservationMetricEvent event) {
        var outcome = transactions.execute(status -> applyInTransaction(event));
        LOGGER.info("campaign.metrics.event eventId={} eventType={} offerId={} outcome={}", event.eventId(),
                event.eventType(), event.offerId(), outcome);
        return outcome;
    }

    @Override
    public CampaignMetrics getMetrics(GetCampaignMetricsQuery query) {
        var campaign = campaignById.getById(new GetCampaignByIdQuery(query.campaignId(), query.businessId()));
        var period = MetricsPeriod.resolve(query.from(), query.to(), campaign.getPeriod(), today());
        var stored = metrics.findDaily(campaign.getId(), period).stream()
                .collect(Collectors.toMap(DailyMetrics::date, Function.identity()));
        var daily = period.dates().stream()
                .map(date -> stored.getOrDefault(date, DailyMetrics.empty(date)))
                .toList();
        var totals = daily.stream().map(DailyMetrics::counts).reduce(MetricCounts.ZERO, MetricCounts::plus);
        var baseline = baselines.findByBusinessId(campaign.getBusinessId()).orElse(null);
        var comparison = baseline == null ? null : baseline.compareWith(totals, period);
        LOGGER.info("campaign.metrics.read campaignId={} from={} to={} baseline={}", campaign.getId(), period.from(),
                period.to(), baseline != null);
        return new CampaignMetrics(campaign, period, daily, totals, baseline, comparison);
    }

    @Override
    public CampaignMetricsSummary list(ListCampaignMetricsQuery query) {
        var period = query.from() == null && query.to() == null
                ? MetricsPeriod.lastDays(SUMMARY_DAYS, today())
                : new MetricsPeriod(query.from() != null ? query.from() : query.to().minusDays(SUMMARY_DAYS - 1L),
                        query.to() != null ? query.to() : today());
        var totals = metrics.totalsByCampaign(query.businessId(), period);
        var rows = campaigns.findByBusinessId(query.businessId()).stream()
                .map(campaign -> new CampaignMetricsSummary.CampaignTotals(campaign,
                        totals.getOrDefault(campaign.getId(), MetricCounts.ZERO)))
                .toList();
        return new CampaignMetricsSummary(period, rows);
    }

    private Outcome applyInTransaction(ReservationMetricEvent event) {
        if (!processedEvents.markProcessed(event.eventId(), event.eventType(), CONSUMER, clock.instant())) {
            return Outcome.DUPLICATE;
        }
        var date = LocalDate.ofInstant(event.occurredAt(), OfferQueryService.OFFER_ZONE);
        var counted = switch (event.kind()) {
            case RESERVED -> metrics.addReservation(event.offerId(), date);
            case REDEEMED -> metrics.addRedemption(event.offerId(), date);
        };
        return counted ? Outcome.APPLIED : Outcome.NOT_COUNTED;
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(OfferQueryService.OFFER_ZONE));
    }
}
