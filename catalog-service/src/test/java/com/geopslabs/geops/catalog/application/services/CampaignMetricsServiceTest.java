package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.ApplyReservationMetricEventUseCase.Outcome;
import com.geopslabs.geops.catalog.application.usecases.GetCampaignByIdUseCase;
import com.geopslabs.geops.catalog.application.usecases.ReservationMetricEvent;
import com.geopslabs.geops.catalog.domain.models.BusinessBaseline;
import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.CampaignStatus;
import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.DailyMetrics;
import com.geopslabs.geops.catalog.domain.models.DateRange;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.MetricCounts;
import com.geopslabs.geops.catalog.domain.models.MetricsPeriod;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.exceptions.CampaignAccessDeniedException;
import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignByIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignMetricsQuery;
import com.geopslabs.geops.catalog.domain.models.queries.ListCampaignMetricsQuery;
import com.geopslabs.geops.catalog.domain.ports.BusinessBaselineRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.CampaignMetricsRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.ProcessedEventsPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignMetricsServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-08T13:05:00Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-10-08");
    private static final Long CAMPAIGN_ID = 14L;
    private static final Long BUSINESS_ID = 84L;
    private static final Long OFFER_ID = 1052L;
    private static final UUID EVENT_ID = UUID.fromString("5b1f8f0e-8a8f-4c62-9a63-0f5a2e1c7d11");
    /** 23:42 of October 20 in Lima. */
    private static final Instant LATE_NIGHT_IN_LIMA = Instant.parse("2026-10-21T04:42:00Z");
    private static final GeoPoint STORE = new GeoPoint(-12.1211, -77.0297);

    @Mock
    private CampaignMetricsRepositoryPort metrics;
    @Mock
    private ProcessedEventsPort processedEvents;
    @Mock
    private GetCampaignByIdUseCase campaignById;
    @Mock
    private CampaignRepositoryPort campaigns;
    @Mock
    private BusinessBaselineRepositoryPort baselines;

    private CampaignMetricsService service;

    @BeforeEach
    void setUp() {
        service = new CampaignMetricsService(metrics, processedEvents, campaignById, campaigns, baselines,
                TransactionOperations.withoutTransaction(), Clock.fixed(NOW, OfferQueryService.OFFER_ZONE));
    }

    @Test
    void aRedemptionAddsToTheCampaignOnItsDayInLima() {
        when(processedEvents.markProcessed(EVENT_ID, "ReservationRedeemed", CampaignMetricsService.CONSUMER, NOW))
                .thenReturn(true);
        when(metrics.addRedemption(OFFER_ID, LocalDate.parse("2026-10-20"))).thenReturn(true);

        var outcome = service.apply(event(ReservationMetricEvent.Kind.REDEEMED, "ReservationRedeemed"));

        assertThat(outcome).isEqualTo(Outcome.APPLIED);
    }

    @Test
    void aReservationAddsAReservation() {
        when(processedEvents.markProcessed(any(), any(), any(), any())).thenReturn(true);
        when(metrics.addReservation(OFFER_ID, LocalDate.parse("2026-10-20"))).thenReturn(true);

        assertThat(service.apply(event(ReservationMetricEvent.Kind.RESERVED, "ReservationCreated")))
                .isEqualTo(Outcome.APPLIED);
        verify(metrics, never()).addRedemption(any(), any());
    }

    @Test
    void aRepeatedEventCountsNothing() {
        when(processedEvents.markProcessed(any(), any(), any(), any())).thenReturn(false);

        assertThat(service.apply(event(ReservationMetricEvent.Kind.REDEEMED, "ReservationRedeemed")))
                .isEqualTo(Outcome.DUPLICATE);
        verifyNoInteractions(metrics);
    }

    @Test
    void anOfferWithoutCampaignIsNotCounted() {
        when(processedEvents.markProcessed(any(), any(), any(), any())).thenReturn(true);
        when(metrics.addRedemption(any(), any())).thenReturn(false);

        assertThat(service.apply(event(ReservationMetricEvent.Kind.REDEEMED, "ReservationRedeemed")))
                .isEqualTo(Outcome.NOT_COUNTED);
    }

    @Test
    void everyDayOfThePeriodIsInTheSeriesWithZerosWhenNothingHappened() {
        givenTheCampaign();
        var period = new MetricsPeriod(LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-07"));
        when(metrics.findDaily(CAMPAIGN_ID, period)).thenReturn(List.of(
                new DailyMetrics(LocalDate.parse("2026-10-05"), new MetricCounts(210, 15, 4, 3)),
                new DailyMetrics(LocalDate.parse("2026-10-07"), new MetricCounts(430, 33, 8, 6))));
        when(baselines.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.empty());

        var result = service.getMetrics(new GetCampaignMetricsQuery(CAMPAIGN_ID, BUSINESS_ID, period.from(),
                period.to()));

        assertThat(result.daily()).extracting(DailyMetrics::date).containsExactlyElementsOf(period.dates());
        assertThat(result.daily().get(1).counts()).isEqualTo(MetricCounts.ZERO);
        assertThat(result.totals()).isEqualTo(new MetricCounts(640, 48, 12, 9));
        assertThat(result.baseline()).isNull();
        assertThat(result.comparison()).isNull();
    }

    @Test
    void withoutDatesThePeriodIsTheValidityUpToTodayAndComparesWithTheBaseline() {
        givenTheCampaign();
        var period = new MetricsPeriod(LocalDate.parse("2026-10-01"), TODAY);
        when(metrics.findDaily(CAMPAIGN_ID, period)).thenReturn(List.of(
                new DailyMetrics(TODAY, new MetricCounts(100, 10, 5, 4))));
        when(baselines.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.of(
                new BusinessBaseline(BUSINESS_ID, 70, new BigDecimal("20.00"), NOW)));

        var result = service.getMetrics(new GetCampaignMetricsQuery(CAMPAIGN_ID, BUSINESS_ID, null, null));

        assertThat(result.period()).isEqualTo(period);
        assertThat(result.comparison().expectedCustomers()).isEqualByComparingTo(new BigDecimal("80.0"));
        assertThat(result.comparison().customersChangePercent()).isEqualByComparingTo(new BigDecimal("5.0"));
    }

    @Test
    void theCampaignOfAnotherBusinessIsNotRead() {
        when(campaignById.getById(new GetCampaignByIdQuery(CAMPAIGN_ID, 99L)))
                .thenThrow(new CampaignAccessDeniedException(CAMPAIGN_ID));

        assertThatThrownBy(() -> service.getMetrics(new GetCampaignMetricsQuery(CAMPAIGN_ID, 99L, null, null)))
                .isInstanceOf(CampaignAccessDeniedException.class);
        verifyNoInteractions(metrics);
    }

    @Test
    void theSummaryKeepsTheCampaignsWithoutActivityWithZeros() {
        var active = campaign(CAMPAIGN_ID);
        var quiet = campaign(15L);
        var period = MetricsPeriod.lastDays(30, TODAY);
        when(campaigns.findByBusinessId(BUSINESS_ID)).thenReturn(List.of(active, quiet));
        when(metrics.totalsByCampaign(BUSINESS_ID, period))
                .thenReturn(Map.of(CAMPAIGN_ID, new MetricCounts(640, 48, 12, 9)));

        var summary = service.list(new ListCampaignMetricsQuery(BUSINESS_ID, null, null));

        assertThat(summary.period()).isEqualTo(period);
        assertThat(summary.campaigns()).hasSize(2);
        assertThat(summary.campaigns().get(1).totals()).isEqualTo(MetricCounts.ZERO);
    }

    private void givenTheCampaign() {
        when(campaignById.getById(new GetCampaignByIdQuery(CAMPAIGN_ID, BUSINESS_ID))).thenReturn(campaign(CAMPAIGN_ID));
    }

    private static Campaign campaign(Long id) {
        return new Campaign(id, BUSINESS_ID, "Almuerzos de octubre", "Menú ejecutivo",
                new DateRange(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31")),
                CampaignZone.radius(STORE, 800), CampaignStatus.ACTIVE, Money.soles(new BigDecimal("500.00")));
    }

    private static ReservationMetricEvent event(ReservationMetricEvent.Kind kind, String eventType) {
        return new ReservationMetricEvent(EVENT_ID, eventType, kind, OFFER_ID, LATE_NIGHT_IN_LIMA);
    }
}
