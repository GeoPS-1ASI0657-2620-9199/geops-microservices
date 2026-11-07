package com.geopslabs.geops.catalog.infrastructure.messaging;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.geopslabs.geops.catalog.domain.events.CampaignPaused;
import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.CampaignStatus;
import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.DateRange;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.infrastructure.persistence.OutboxEventJpaEntity;
import com.geopslabs.geops.catalog.infrastructure.persistence.OutboxEventJpaRepository;
import com.geopslabs.geops.catalog.infrastructure.persistence.OutboxJpaAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CampaignPausedContractTest {
    private static final Instant PAUSED_AT = Instant.parse("2026-10-28T10:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d");
    private static final Long CAMPAIGN_ID = 77L;
    private static final Long BUSINESS_ID = 84L;
    private static final int RADIUS_METERS = 1200;
    private static final List<Long> OFFER_IDS = List.of(1052L, 1053L);

    private final OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
    private final OutboxJpaAdapter adapter = new OutboxJpaAdapter(repository, Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build(),
            Clock.fixed(PAUSED_AT, ZoneOffset.UTC));

    @Test
    void campaignPausedHasTheShapeOfTheExample() {
        adapter.append(CampaignPaused.of(campaign(), OFFER_IDS, EVENT_ID, PAUSED_AT));

        var saved = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(saved.capture());
        EventContract.assertSameShape(saved.getValue().getPayload(), CampaignPaused.EVENT_TYPE);
        assertThat(saved.getValue().getAggregateId()).isEqualTo(CAMPAIGN_ID);
        assertThat(saved.getValue().getPayload()).contains("\"offerIds\":[1052,1053]",
                "\"pausedAt\":\"2026-10-28T10:00:00Z\"");
    }

    private static Campaign campaign() {
        var period = new DateRange(LocalDate.parse("2026-10-27"), LocalDate.parse("2026-11-15"));
        return new Campaign(CAMPAIGN_ID, BUSINESS_ID, "Almuerzos de noviembre", "Menú ejecutivo 2x1", period,
                CampaignZone.radius(new GeoPoint(-12.1211, -77.0297), RADIUS_METERS), CampaignStatus.PAUSED,
                Money.soles(BigDecimal.TEN));
    }
}
