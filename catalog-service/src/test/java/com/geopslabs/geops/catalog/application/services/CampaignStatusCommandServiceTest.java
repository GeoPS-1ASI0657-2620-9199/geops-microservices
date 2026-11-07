package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.ChangeCampaignStatusCommand;
import com.geopslabs.geops.catalog.domain.events.CampaignPaused;
import com.geopslabs.geops.catalog.domain.events.DomainEvent;
import com.geopslabs.geops.catalog.domain.events.OfferPublished;
import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.CampaignStatus;
import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.DateRange;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import com.geopslabs.geops.catalog.domain.models.exceptions.CampaignAccessDeniedException;
import com.geopslabs.geops.catalog.domain.models.exceptions.CampaignEndedException;
import com.geopslabs.geops.catalog.domain.models.exceptions.CampaignNotFoundException;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidCampaignStatusException;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignStatusCommandServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-28T15:00:00Z");
    private static final Instant AFTER_THE_END = Instant.parse("2026-11-16T15:00:00Z");
    private static final Long CAMPAIGN_ID = 77L;
    private static final Long BUSINESS_ID = 84L;
    private static final Long OTHER_BUSINESS_ID = 99L;
    private static final Long VALID_OFFER_ID = 1052L;
    private static final Long EXPIRED_OFFER_ID = 1053L;
    private static final String BUSINESS_NAME = "Restaurante Don Pepe";
    private static final GeoPoint STORE = new GeoPoint(-12.1211, -77.0297);
    private static final int RADIUS_METERS = 1200;
    private static final DateRange PERIOD = new DateRange(LocalDate.parse("2026-10-27"), LocalDate.parse("2026-11-15"));

    @Mock
    private CampaignRepositoryPort campaignRepository;

    @Mock
    private OfferRepositoryPort offerRepository;

    @Mock
    private MerchantStandingRepositoryPort merchantStandingRepository;

    @Mock
    private OutboxPort outbox;

    @Mock
    private CellInvalidator cellInvalidator;

    private CampaignStatusCommandService service;

    @BeforeEach
    void setUp() {
        service = serviceAt(NOW);
    }

    @Test
    void pausingSavesTheCampaignAndAnnouncesItWithAllItsOffers() {
        stubCampaign(CampaignStatus.ACTIVE);
        stubSaveAndOffers();

        var paused = service.changeStatus(command(CampaignStatus.PAUSED));

        assertThat(paused.getStatus()).isEqualTo(CampaignStatus.PAUSED);
        verify(campaignRepository).save(any());
        var event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outbox).append(event.capture());
        var campaignPaused = (CampaignPaused) event.getValue();
        assertThat(campaignPaused.aggregateId()).isEqualTo(CAMPAIGN_ID);
        assertThat(campaignPaused.payload().businessId()).isEqualTo(BUSINESS_ID);
        assertThat(campaignPaused.payload().offerIds()).containsExactly(VALID_OFFER_ID, EXPIRED_OFFER_ID);
        assertThat(campaignPaused.payload().pausedAt()).isEqualTo(NOW);
    }

    @Test
    void pausingDeletesTheCellsOfTheCampaignOffers() {
        stubCampaign(CampaignStatus.ACTIVE);
        stubSaveAndOffers();

        service.changeStatus(command(CampaignStatus.PAUSED));

        verify(cellInvalidator).evictCellsOf(List.of(STORE, STORE));
    }

    @Test
    void reactivatingAnnouncesEveryOfferStillValidAsReactivated() {
        stubCampaign(CampaignStatus.PAUSED);
        stubSaveAndOffers();
        when(merchantStandingRepository.findByBusinessId(BUSINESS_ID))
                .thenReturn(Optional.of(MerchantStanding.provisional(BUSINESS_ID, BUSINESS_NAME)));

        var active = service.changeStatus(command(CampaignStatus.ACTIVE));

        assertThat(active.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
        var event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outbox).append(event.capture());
        var published = (OfferPublished) event.getValue();
        assertThat(published.payload().offerId()).isEqualTo(VALID_OFFER_ID);
        assertThat(published.payload().reactivated()).isTrue();
        assertThat(published.payload().businessName()).isEqualTo(BUSINESS_NAME);
        verify(cellInvalidator).evictCellsOf(List.of(STORE, STORE));
    }

    @Test
    void askingForTheCurrentStatusWritesNothing() {
        stubCampaign(CampaignStatus.PAUSED);

        var unchanged = service.changeStatus(command(CampaignStatus.PAUSED));

        assertThat(unchanged.getStatus()).isEqualTo(CampaignStatus.PAUSED);
        verify(campaignRepository, never()).save(any());
        verifyNoInteractions(offerRepository, outbox, cellInvalidator);
    }

    @Test
    void aCampaignWhoseValidityEndedIsNotReactivated() {
        service = serviceAt(AFTER_THE_END);
        stubCampaign(CampaignStatus.PAUSED);

        assertThatThrownBy(() -> service.changeStatus(command(CampaignStatus.ACTIVE)))
                .isInstanceOf(CampaignEndedException.class);
        verify(campaignRepository, never()).save(any());
        verifyNoInteractions(outbox, cellInvalidator);
    }

    @Test
    void theCampaignOfAnotherBusinessIsNotChanged() {
        stubCampaign(CampaignStatus.ACTIVE);
        var foreign = new ChangeCampaignStatusCommand(CAMPAIGN_ID, OTHER_BUSINESS_ID, CampaignStatus.PAUSED);

        assertThatThrownBy(() -> service.changeStatus(foreign)).isInstanceOf(CampaignAccessDeniedException.class);
        verify(campaignRepository, never()).save(any());
        verifyNoInteractions(outbox, cellInvalidator);
    }

    @Test
    void anUnknownCampaignIsNotFound() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(command(CampaignStatus.PAUSED)))
                .isInstanceOf(CampaignNotFoundException.class);
    }

    @Test
    void finishedIsNotAStatusTheOwnerCanAskFor() {
        stubCampaign(CampaignStatus.ACTIVE);

        assertThatThrownBy(() -> service.changeStatus(command(CampaignStatus.FINISHED)))
                .isInstanceOf(InvalidCampaignStatusException.class);
    }

    private CampaignStatusCommandService serviceAt(Instant now) {
        return new CampaignStatusCommandService(campaignRepository, offerRepository, merchantStandingRepository,
                outbox, cellInvalidator, Clock.fixed(now, OfferQueryService.OFFER_ZONE));
    }

    private void stubCampaign(CampaignStatus status) {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign(status)));
    }

    private void stubSaveAndOffers() {
        when(campaignRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(offerRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of(
                offer(VALID_OFFER_ID, OfferStatus.PUBLISHED), offer(EXPIRED_OFFER_ID, OfferStatus.EXPIRED)));
    }

    private static ChangeCampaignStatusCommand command(CampaignStatus status) {
        return new ChangeCampaignStatusCommand(CAMPAIGN_ID, BUSINESS_ID, status);
    }

    private static Campaign campaign(CampaignStatus status) {
        return new Campaign(CAMPAIGN_ID, BUSINESS_ID, "Almuerzos de noviembre", "Menú ejecutivo 2x1", PERIOD,
                CampaignZone.radius(STORE, RADIUS_METERS), status, Money.soles(BigDecimal.TEN));
    }

    private static Offer offer(Long id, OfferStatus status) {
        return new Offer(id, CAMPAIGN_ID, BUSINESS_ID, "2x1 en almuerzos ejecutivos", "Lunes a viernes",
                Money.soles(new BigDecimal("18.50")), PERIOD.end(), "Gastronomía", GeocodingStatus.GEOCODED,
                "Av. Larco 345, Miraflores", null, OfferSource.AFFILIATED, null, status, STORE);
    }
}
