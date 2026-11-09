package com.geopslabs.geops.catalog.infrastructure.messaging;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.geopslabs.geops.catalog.domain.events.OfferPublished;
import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.CampaignStatus;
import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.DateRange;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OfferPublishedContractTest {
    private static final Instant PUBLISHED_AT = Instant.parse("2026-10-27T15:04:05Z");
    private static final UUID EVENT_ID = UUID.fromString("8e0f1c64-2a51-4f0b-9b8e-6f1d1c2a7e10");
    private static final Long CAMPAIGN_ID = 77L;
    private static final Long BUSINESS_ID = 84L;
    private static final String BUSINESS_NAME = "Restaurante Don Pepe";
    private static final GeoPoint STORE = new GeoPoint(-12.1211, -77.0297);
    private static final GeoPoint ZONE_CENTER = new GeoPoint(-12.1205, -77.0301);
    private static final int RADIUS_METERS = 1200;
    private static final LocalDate START = LocalDate.parse("2026-10-27");
    private static final LocalDate END = LocalDate.parse("2026-11-15");
    private static final boolean FIRST_PUBLICATION = false;
    private static final boolean REACTIVATION = true;

    private final OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
    private final OutboxJpaAdapter adapter = new OutboxJpaAdapter(repository, Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build(),
            Clock.fixed(PUBLISHED_AT, ZoneOffset.UTC));

    @Test
    void offerPublishedHasTheShapeOfTheExample() {
        adapter.append(OfferPublished.of(campaign(CampaignZone.radius(ZONE_CENTER, RADIUS_METERS)), offer(),
                BUSINESS_NAME, FIRST_PUBLICATION, EVENT_ID, PUBLISHED_AT));

        EventContract.assertSameShape(written().getPayload(), OfferPublished.EVENT_TYPE);
        assertThat(written().getAggregateId()).isEqualTo(CAMPAIGN_ID);
        assertThat(written().getPayload()).contains("\"occurredAt\":\"2026-10-27T15:04:05Z\"",
                "\"validTo\":\"2026-11-15\"", "\"price\":18.50", "\"reachMeters\":1200", "\"reactivated\":false");
    }

    @Test
    void aDistrictZoneReachesTheCoverageOfADistrict() {
        adapter.append(OfferPublished.of(campaign(CampaignZone.district("Miraflores", ZONE_CENTER)), offer(),
                BUSINESS_NAME, REACTIVATION, EVENT_ID, PUBLISHED_AT));

        EventContract.assertSameShape(written().getPayload(), OfferPublished.EVENT_TYPE);
        assertThat(written().getPayload()).contains("\"type\":\"DISTRICT\"", "\"district\":\"Miraflores\"",
                "\"reachMeters\":" + CampaignZone.DISTRICT_COVERAGE_METERS, "\"reactivated\":true");
    }

    private OutboxEventJpaEntity written() {
        var saved = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(saved.capture());
        return saved.getValue();
    }

    private static Campaign campaign(CampaignZone zone) {
        return new Campaign(CAMPAIGN_ID, BUSINESS_ID, "Almuerzos de noviembre", "Menú ejecutivo 2x1",
                new DateRange(START, END), zone, CampaignStatus.ACTIVE, Money.soles(new BigDecimal("300.00")));
    }

    private static Offer offer() {
        return new Offer(1052L, CAMPAIGN_ID, BUSINESS_ID, "2x1 en almuerzos ejecutivos", "Lunes a viernes",
                Money.soles(new BigDecimal("18.50")), END, "Gastronomía", GeocodingStatus.GEOCODED,
                "Av. Larco 345, Miraflores", null, OfferSource.AFFILIATED, null, OfferStatus.PUBLISHED, STORE);
    }
}
