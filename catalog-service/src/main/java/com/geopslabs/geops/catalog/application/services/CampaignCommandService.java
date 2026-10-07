package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.CreateCampaignCommand;
import com.geopslabs.geops.catalog.application.usecases.CreateCampaignUseCase;
import com.geopslabs.geops.catalog.application.usecases.CreateOfferCommand;
import com.geopslabs.geops.catalog.application.usecases.PublishedCampaign;
import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.DateRange;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.ZoneType;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Transactional
public class CampaignCommandService implements CreateCampaignUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignCommandService.class);
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final CampaignRepositoryPort campaignRepository;
    private final OfferRepositoryPort offerRepository;
    private final MerchantStandingRepositoryPort merchantStandingRepository;
    private final Clock clock;

    public CampaignCommandService(CampaignRepositoryPort campaignRepository, OfferRepositoryPort offerRepository,
                                  MerchantStandingRepositoryPort merchantStandingRepository, Clock clock) {
        this.campaignRepository = campaignRepository;
        this.offerRepository = offerRepository;
        this.merchantStandingRepository = merchantStandingRepository;
        this.clock = clock;
    }

    @Override
    public PublishedCampaign publish(CreateCampaignCommand command) {
        var campaign = Campaign.publish(command.businessId(), command.name(), command.description(),
                new DateRange(command.start(), command.end()), zoneOf(command), budgetOf(command), today());
        var offers = command.offers().stream().map(offer -> offerOf(campaign, offer, command)).toList();
        ensureMerchantStanding(command.businessId(), command.businessName());
        var saved = campaignRepository.save(campaign);
        var savedOffers = addOffers(saved, offers);
        LOGGER.info("campaign.published campaignId={} businessId={} offers={}", saved.getId(), saved.getBusinessId(),
                savedOffers.size());
        return new PublishedCampaign(saved, savedOffers);
    }

    private void ensureMerchantStanding(Long businessId, String businessName) {
        if (merchantStandingRepository.findByBusinessId(businessId).isEmpty()) {
            merchantStandingRepository.save(MerchantStanding.provisional(businessId, businessName,
                    LocalDateTime.now(clock.withZone(LIMA))));
        }
    }

    private List<Offer> addOffers(Campaign campaign, List<Offer> offers) {
        return offers.stream().map(offer -> offerRepository.save(offer.inCampaign(campaign.getId()))).toList();
    }

    private static Offer offerOf(Campaign campaign, CreateOfferCommand offer, CreateCampaignCommand command) {
        return Offer.publishFor(campaign, offer.title(), offer.conditions(), Money.soles(offer.price()),
                offer.validTo(), offer.category(), offer.imageUrl(), command.storeAddress(), command.storeLocation());
    }

    private static CampaignZone zoneOf(CreateCampaignCommand command) {
        if (command.zoneType() == ZoneType.DISTRICT) {
            return CampaignZone.district(command.district(), command.zoneCenter());
        }
        return CampaignZone.radius(command.zoneCenter(), command.radiusMeters());
    }

    private static Money budgetOf(CreateCampaignCommand command) {
        return Money.soles(Optional.ofNullable(command.estimatedBudget()).orElse(BigDecimal.ZERO));
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(LIMA));
    }
}
