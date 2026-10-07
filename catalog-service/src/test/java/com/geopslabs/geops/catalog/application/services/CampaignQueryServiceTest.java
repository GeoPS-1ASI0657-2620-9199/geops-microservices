package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.CampaignStatus;
import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.DateRange;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.ZoneType;
import com.geopslabs.geops.catalog.domain.models.exceptions.CampaignAccessDeniedException;
import com.geopslabs.geops.catalog.domain.models.exceptions.CampaignNotFoundException;
import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignByIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignsByBusinessIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetOffersByCampaignIdQuery;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignQueryServiceTest {
    private static final Long CAMPAIGN_ID = 14L;
    private static final Long BUSINESS_ID = 1L;
    private static final Long OTHER_BUSINESS_ID = 2L;
    private static final Integer RADIUS_METERS = 800;
    private static final BigDecimal BUDGET = new BigDecimal("500.00");

    @Mock
    private CampaignRepositoryPort campaignRepository;

    @Mock
    private OfferRepositoryPort offerRepository;

    private CampaignQueryService service;

    @BeforeEach
    void setUp() {
        service = new CampaignQueryService(campaignRepository, offerRepository);
    }

    @Test
    void returnsTheCampaignOfTheBusiness() {
        var campaign = campaign();
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

        assertThat(service.getById(new GetCampaignByIdQuery(CAMPAIGN_ID, BUSINESS_ID))).isSameAs(campaign);
    }

    @Test
    void unknownCampaignIsNotFound() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(new GetCampaignByIdQuery(CAMPAIGN_ID, BUSINESS_ID)))
                .isInstanceOf(CampaignNotFoundException.class)
                .hasMessage("Campaign 14 was not found");
    }

    @Test
    void campaignOfAnotherBusinessIsForbidden() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign()));

        assertThatThrownBy(() -> service.getById(new GetCampaignByIdQuery(CAMPAIGN_ID, OTHER_BUSINESS_ID)))
                .isInstanceOf(CampaignAccessDeniedException.class);
    }

    @Test
    void listsTheCampaignsOfTheBusiness() {
        var campaigns = List.of(campaign());
        when(campaignRepository.findByBusinessId(BUSINESS_ID)).thenReturn(campaigns);

        assertThat(service.list(new GetCampaignsByBusinessIdQuery(BUSINESS_ID))).isEqualTo(campaigns);
    }

    @Test
    void listsTheOffersOfACampaignOfTheBusiness() {
        var offers = List.of(mock(Offer.class));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign()));
        when(offerRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(offers);

        assertThat(service.list(new GetOffersByCampaignIdQuery(CAMPAIGN_ID, BUSINESS_ID))).isEqualTo(offers);
    }

    @Test
    void doesNotListTheOffersOfACampaignOfAnotherBusiness() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign()));

        assertThatThrownBy(() -> service.list(new GetOffersByCampaignIdQuery(CAMPAIGN_ID, OTHER_BUSINESS_ID)))
                .isInstanceOf(CampaignAccessDeniedException.class);
        verify(offerRepository, never()).findByCampaignId(any());
    }

    private static Campaign campaign() {
        var period = new DateRange(LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-31"));
        var zone = new CampaignZone(ZoneType.RADIUS, null, RADIUS_METERS, null);
        return new Campaign(CAMPAIGN_ID, BUSINESS_ID, "Almuerzos de octubre", "Menú ejecutivo a mitad de precio",
                period, zone, CampaignStatus.ACTIVE, Money.soles(BUDGET));
    }
}
