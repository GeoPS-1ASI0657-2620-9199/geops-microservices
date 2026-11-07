package com.geopslabs.geops.catalog.infrastructure.messaging;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.geopslabs.geops.catalog.domain.events.OfferExpired;
import com.geopslabs.geops.catalog.domain.models.ExpiredOffer;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.infrastructure.persistence.OutboxEventJpaEntity;
import com.geopslabs.geops.catalog.infrastructure.persistence.OutboxEventJpaRepository;
import com.geopslabs.geops.catalog.infrastructure.persistence.OutboxJpaAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OfferExpiredContractTest {
    private static final Instant EXPIRED_AT = Instant.parse("2026-11-16T05:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("6f5e4d3c-2b1a-4c0d-9e8f-7a6b5c4d3e2f");
    private static final Long CAMPAIGN_ID = 77L;
    private static final ExpiredOffer CAMPAIGN_OFFER = new ExpiredOffer(1052L, CAMPAIGN_ID, 84L,
            LocalDate.parse("2026-11-15"), new GeoPoint(-12.1211, -77.0297));

    private final OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
    private final OutboxJpaAdapter adapter = new OutboxJpaAdapter(repository, Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build(),
            Clock.fixed(EXPIRED_AT, ZoneOffset.UTC));

    @Test
    void offerExpiredHasTheShapeOfTheExample() {
        adapter.append(OfferExpired.of(CAMPAIGN_OFFER, EVENT_ID, EXPIRED_AT));

        var saved = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(saved.capture());
        EventContract.assertSameShape(saved.getValue().getPayload(), OfferExpired.EVENT_TYPE);
        assertThat(saved.getValue().getAggregateId()).isEqualTo(CAMPAIGN_ID);
        assertThat(saved.getValue().getPayload()).contains("\"validTo\":\"2026-11-15\"",
                "\"expiredAt\":\"2026-11-16T05:00:00Z\"");
    }

    @Test
    void aPublicSourceOfferHasNoEvent() {
        var publicOffer = new ExpiredOffer(2001L, null, null, LocalDate.parse("2026-11-15"), null);

        assertThatThrownBy(() -> OfferExpired.of(publicOffer, EVENT_ID, EXPIRED_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
