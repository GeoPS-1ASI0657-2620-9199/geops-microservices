package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.CreateCampaignCommand;
import com.geopslabs.geops.catalog.application.usecases.CreateOfferCommand;
import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.ZoneType;
import com.geopslabs.geops.catalog.domain.models.exceptions.CampaignAlreadyEndedException;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidCampaignZoneException;
import com.geopslabs.geops.catalog.domain.models.exceptions.OfferValidityOutsideCampaignException;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
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
import java.time.ZoneId;
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
class CampaignCommandServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-08T13:05:00Z");
    private static final Long BUSINESS_ID = 84L;
    private static final Long CAMPAIGN_ID = 31L;
    private static final String BUSINESS_NAME = "Restaurante Don Pepe";
    private static final String ADDRESS = "Av. Larco 345, Miraflores";
    private static final GeoPoint STORE = new GeoPoint(-12.1211, -77.0297);
    private static final int RADIUS_METERS = 800;
    private static final String DISTRICT = "Miraflores";
    private static final BigDecimal BUDGET = new BigDecimal("500.00");
    private static final LocalDate START = LocalDate.parse("2026-10-05");
    private static final LocalDate END = LocalDate.parse("2026-10-31");
    private static final LocalDate OFFER_VALID_TO = LocalDate.parse("2026-10-15");

    @Mock
    private CampaignRepositoryPort campaignRepository;

    @Mock
    private OfferRepositoryPort offerRepository;

    @Mock
    private MerchantStandingRepositoryPort merchantStandingRepository;

    private CampaignCommandService service;

    @BeforeEach
    void setUp() {
        service = new CampaignCommandService(campaignRepository, offerRepository, merchantStandingRepository,
                Clock.fixed(NOW, ZoneId.of("America/Lima")));
    }

    @Test
    void firstCampaignOfABusinessCreatesItsProvisionalStanding() {
        stubSaves();
        when(merchantStandingRepository.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.empty());

        service.publish(command(START, END, BUDGET, ZoneType.RADIUS, OFFER_VALID_TO));

        var standing = ArgumentCaptor.forClass(MerchantStanding.class);
        verify(merchantStandingRepository).save(standing.capture());
        assertThat(standing.getValue().getBusinessId()).isEqualTo(BUSINESS_ID);
        assertThat(standing.getValue().getBusinessName()).isEqualTo(BUSINESS_NAME);
        assertThat(standing.getValue().hasVerifiedSeal()).isFalse();
        assertThat(standing.getValue().getComplianceIndex()).isEqualByComparingTo(
                MerchantStanding.INITIAL_COMPLIANCE_INDEX);
    }

    @Test
    void existingStandingIsKept() {
        stubSaves();
        when(merchantStandingRepository.findByBusinessId(BUSINESS_ID))
                .thenReturn(Optional.of(MerchantStanding.provisional(BUSINESS_ID, BUSINESS_NAME, null)));

        service.publish(command(START, END, BUDGET, ZoneType.RADIUS, OFFER_VALID_TO));

        verify(merchantStandingRepository, never()).save(any());
    }

    @Test
    void everyOfferGetsTheCampaignBusinessAndTheStoreLocation() {
        stubSaves();

        var published = service.publish(command(START, END, BUDGET, ZoneType.RADIUS, OFFER_VALID_TO));

        assertThat(published.campaign().getId()).isEqualTo(CAMPAIGN_ID);
        assertThat(published.offers()).singleElement().satisfies(offer -> {
            assertThat(offer.getCampaignId()).isEqualTo(CAMPAIGN_ID);
            assertThat(offer.getBusinessId()).isEqualTo(BUSINESS_ID);
            assertThat(offer.getAddress()).isEqualTo(ADDRESS);
            assertThat(offer.getLocation()).isEqualTo(STORE);
        });
    }

    @Test
    void campaignWithoutBudgetIsSavedWithZero() {
        stubSaves();

        var published = service.publish(command(START, END, null, ZoneType.RADIUS, OFFER_VALID_TO));

        assertThat(published.campaign().getEstimatedBudget().amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void nothingIsSavedWhenTheValidityAlreadyEnded() {
        var september = command(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-30"), BUDGET,
                ZoneType.RADIUS, LocalDate.parse("2026-09-30"));

        assertThatThrownBy(() -> service.publish(september)).isInstanceOf(CampaignAlreadyEndedException.class);
        verifyNoInteractions(merchantStandingRepository, campaignRepository, offerRepository);
    }

    @Test
    void nothingIsSavedWhenAnOfferIsOutsideThePeriod() {
        var outside = command(START, END, BUDGET, ZoneType.RADIUS, LocalDate.parse("2026-11-15"));

        assertThatThrownBy(() -> service.publish(outside)).isInstanceOf(OfferValidityOutsideCampaignException.class);
        verifyNoInteractions(merchantStandingRepository, campaignRepository, offerRepository);
    }

    @Test
    void districtZoneIsSavedWithItsNameAndCenter() {
        stubSaves();

        var published = service.publish(districtCommand(DISTRICT));

        assertThat(published.campaign().getZone().type()).isEqualTo(ZoneType.DISTRICT);
        assertThat(published.campaign().getZone().district()).isEqualTo(DISTRICT);
        assertThat(published.campaign().getZone().center()).isEqualTo(STORE);
        assertThat(published.campaign().getZone().radiusMeters()).isNull();
    }

    @Test
    void nothingIsSavedWhenTheDistrictHasNoName() {
        var withoutName = districtCommand(null);

        assertThatThrownBy(() -> service.publish(withoutName)).isInstanceOf(InvalidCampaignZoneException.class);
        verifyNoInteractions(merchantStandingRepository, campaignRepository, offerRepository);
    }

    @Test
    void nothingIsSavedWhenTheRadiusZoneHasNoRadius() {
        var withoutRadius = new CreateCampaignCommand(BUSINESS_ID, BUSINESS_NAME, "Almuerzos de octubre",
                "Menú ejecutivo", START, END, BUDGET, ADDRESS, STORE, ZoneType.RADIUS, STORE, null, null,
                List.of(offer(OFFER_VALID_TO)));

        assertThatThrownBy(() -> service.publish(withoutRadius)).isInstanceOf(InvalidCampaignZoneException.class);
        verifyNoInteractions(merchantStandingRepository, campaignRepository, offerRepository);
    }

    private void stubSaves() {
        when(campaignRepository.save(any())).thenAnswer(call -> withId(call.getArgument(0)));
        when(offerRepository.save(any())).thenAnswer(call -> call.getArgument(0, Offer.class));
    }

    private static Campaign withId(Campaign campaign) {
        return new Campaign(CAMPAIGN_ID, campaign.getBusinessId(), campaign.getName(), campaign.getDescription(),
                campaign.getPeriod(), campaign.getZone(), campaign.getStatus(), campaign.getEstimatedBudget());
    }

    private static CreateCampaignCommand command(LocalDate start, LocalDate end, BigDecimal budget,
                                                 ZoneType zoneType, LocalDate offerValidTo) {
        return new CreateCampaignCommand(BUSINESS_ID, BUSINESS_NAME, "Almuerzos de octubre", "Menú ejecutivo",
                start, end, budget, ADDRESS, STORE, zoneType, STORE, RADIUS_METERS, null,
                List.of(offer(offerValidTo)));
    }

    private static CreateCampaignCommand districtCommand(String district) {
        return new CreateCampaignCommand(BUSINESS_ID, BUSINESS_NAME, "Ceviches en Miraflores", "Ceviche 2x1",
                START, END, BUDGET, ADDRESS, STORE, ZoneType.DISTRICT, STORE, null, district,
                List.of(offer(OFFER_VALID_TO)));
    }

    private static CreateOfferCommand offer(LocalDate validTo) {
        return new CreateOfferCommand("2x1 en almuerzos ejecutivos", "Lunes a viernes de 12:00 a 15:00",
                new BigDecimal("15.00"), validTo, "Gastronomía", null);
    }
}
