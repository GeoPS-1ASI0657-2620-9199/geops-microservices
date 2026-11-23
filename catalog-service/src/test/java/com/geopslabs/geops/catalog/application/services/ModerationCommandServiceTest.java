package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.RemoveOfferCommand;
import com.geopslabs.geops.catalog.domain.events.DomainEvent;
import com.geopslabs.geops.catalog.domain.events.OfferRemoved;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.ModerationAction;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidModerationReasonException;
import com.geopslabs.geops.catalog.domain.models.exceptions.OfferAlreadyRemovedException;
import com.geopslabs.geops.catalog.domain.models.exceptions.OfferNotFoundException;
import com.geopslabs.geops.catalog.domain.ports.ModerationRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OutboxPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModerationCommandServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-22T15:04:05Z");
    private static final Long OFFER_ID = 1052L;
    private static final Long CAMPAIGN_ID = 77L;
    private static final Long BUSINESS_ID = 84L;
    private static final Long ADMIN_ID = 1L;
    private static final Long MODERATION_ID = 31L;
    private static final String REASON = "La oferta anuncia un precio que el local no respeta.";
    private static final GeoPoint STORE = new GeoPoint(-12.1211, -77.0297);

    @Mock
    private OfferRepositoryPort offers;

    @Mock
    private ModerationRepositoryPort moderation;

    @Mock
    private OutboxPort outbox;

    @Mock
    private CellInvalidator cellInvalidator;

    private ModerationCommandService service;

    @BeforeEach
    void setUp() {
        service = new ModerationCommandService(offers, moderation, outbox, cellInvalidator,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void removingACampaignOfferRecordsTheActionWritesOfferRemovedAndDeletesItsCell() {
        when(offers.findById(OFFER_ID)).thenReturn(Optional.of(campaignOffer(OfferStatus.PUBLISHED)));
        when(offers.markRemoved(OFFER_ID)).thenReturn(true);
        when(moderation.save(any())).thenAnswer(call -> call.<ModerationAction>getArgument(0).withId(MODERATION_ID));

        var action = service.remove(new RemoveOfferCommand(OFFER_ID, ADMIN_ID, REASON));

        assertThat(action).isEqualTo(new ModerationAction(MODERATION_ID, OFFER_ID, ADMIN_ID, REASON, NOW));
        var event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outbox).append(event.capture());
        assertThat(event.getValue()).isInstanceOfSatisfying(OfferRemoved.class, removed -> {
            assertThat(removed.aggregateId()).isEqualTo(CAMPAIGN_ID);
            assertThat(removed.payload()).isEqualTo(new OfferRemoved.Payload(OFFER_ID, CAMPAIGN_ID, BUSINESS_ID, NOW));
            assertThat(removed.occurredAt()).isEqualTo(NOW);
        });
        verify(cellInvalidator).evictCellsOf(List.of(STORE));
    }

    @Test
    void aPublicSourceOfferWithoutCoordinatesIsRemovedWithoutEvent() {
        var publicOffer = new Offer(OFFER_ID, null, null, "Menú del día", "Solo en el local",
                Money.soles(BigDecimal.TEN), LocalDate.parse("2026-11-30"), "Gastronomía", GeocodingStatus.PENDING,
                "Av. Grau 120, La Victoria", null, OfferSource.PUBLIC_SOURCE, "Diario El Barrio",
                OfferStatus.PUBLISHED, null);
        when(offers.findById(OFFER_ID)).thenReturn(Optional.of(publicOffer));
        when(offers.markRemoved(OFFER_ID)).thenReturn(true);
        when(moderation.save(any())).thenAnswer(call -> call.getArgument(0));

        service.remove(new RemoveOfferCommand(OFFER_ID, ADMIN_ID, REASON));

        verifyNoInteractions(outbox);
        verify(cellInvalidator).evictCellsOf(List.of());
    }

    @Test
    void anUnknownOfferIsNotFound() {
        when(offers.findById(OFFER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(new RemoveOfferCommand(OFFER_ID, ADMIN_ID, REASON)))
                .isInstanceOf(OfferNotFoundException.class);
        verifyNoInteractions(moderation, outbox, cellInvalidator);
    }

    @Test
    void anOfferAlreadyRemovedIsAConflict() {
        when(offers.findById(OFFER_ID)).thenReturn(Optional.of(campaignOffer(OfferStatus.REMOVED)));

        assertThatThrownBy(() -> service.remove(new RemoveOfferCommand(OFFER_ID, ADMIN_ID, REASON)))
                .isInstanceOf(OfferAlreadyRemovedException.class);
        verify(offers, never()).markRemoved(anyLong());
        verifyNoInteractions(moderation, outbox, cellInvalidator);
    }

    @Test
    void anotherAdministratorRemovingItFirstIsAConflict() {
        when(offers.findById(OFFER_ID)).thenReturn(Optional.of(campaignOffer(OfferStatus.PUBLISHED)));
        when(offers.markRemoved(OFFER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.remove(new RemoveOfferCommand(OFFER_ID, ADMIN_ID, REASON)))
                .isInstanceOf(OfferAlreadyRemovedException.class);
        verifyNoInteractions(moderation, outbox, cellInvalidator);
    }

    @Test
    void aBlankReasonChangesNothing() {
        when(offers.findById(OFFER_ID)).thenReturn(Optional.of(campaignOffer(OfferStatus.PUBLISHED)));

        assertThatThrownBy(() -> service.remove(new RemoveOfferCommand(OFFER_ID, ADMIN_ID, "   ")))
                .isInstanceOf(InvalidModerationReasonException.class);
        verify(offers, never()).markRemoved(anyLong());
        verifyNoInteractions(moderation, outbox, cellInvalidator);
    }

    private static Offer campaignOffer(OfferStatus status) {
        return new Offer(OFFER_ID, CAMPAIGN_ID, BUSINESS_ID, "2x1 en almuerzos ejecutivos", "Lunes a viernes",
                Money.soles(new BigDecimal("18.50")), LocalDate.parse("2026-11-30"), "Gastronomía",
                GeocodingStatus.GEOCODED, "Av. Larco 345, Miraflores", null, OfferSource.AFFILIATED, null, status,
                STORE);
    }
}
