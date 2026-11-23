package com.geopslabs.geops.catalog.infrastructure.messaging;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.geopslabs.geops.catalog.domain.events.OfferRemoved;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.ModerationAction;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OfferRemovedContractTest {
    private static final Instant REMOVED_AT = Instant.parse("2026-11-22T15:04:05Z");
    private static final UUID EVENT_ID = UUID.fromString("8b7a6c5d-4e3f-4a2b-9c1d-0e9f8a7b6c5d");
    private static final Long CAMPAIGN_ID = 77L;
    private static final ModerationAction ACTION = new ModerationAction(31L, 1052L, 1L,
            "La oferta anuncia un precio que el local no respeta.", REMOVED_AT);

    private final OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
    private final OutboxJpaAdapter adapter = new OutboxJpaAdapter(repository, Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build(),
            Clock.fixed(REMOVED_AT, ZoneOffset.UTC));

    @Test
    void offerRemovedHasTheShapeOfTheExampleAndKeepsTheReasonInCatalog() {
        adapter.append(OfferRemoved.of(offer(CAMPAIGN_ID, 84L, OfferSource.AFFILIATED, null), ACTION, EVENT_ID));

        var saved = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(saved.capture());
        EventContract.assertSameShape(saved.getValue().getPayload(), OfferRemoved.EVENT_TYPE);
        assertThat(saved.getValue().getAggregateId()).isEqualTo(CAMPAIGN_ID);
        assertThat(saved.getValue().getPayload()).contains("\"removedAt\":\"2026-11-22T15:04:05Z\"")
                .doesNotContain("reason");
    }

    @Test
    void aPublicSourceOfferHasNoEvent() {
        var publicOffer = offer(null, null, OfferSource.PUBLIC_SOURCE, "Diario El Barrio");

        assertThatThrownBy(() -> OfferRemoved.of(publicOffer, ACTION, EVENT_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Offer offer(Long campaignId, Long businessId, OfferSource source, String sourceName) {
        return new Offer(1052L, campaignId, businessId, "2x1 en almuerzos ejecutivos", "Lunes a viernes",
                Money.soles(new BigDecimal("18.50")), LocalDate.parse("2026-11-30"), "Gastronomía",
                GeocodingStatus.GEOCODED, "Av. Larco 345, Miraflores", null, source, sourceName,
                OfferStatus.PUBLISHED, null);
    }
}
