package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.ChangeCampaignStatusCommand;
import com.geopslabs.geops.catalog.application.usecases.ChangeCampaignStatusUseCase;
import com.geopslabs.geops.catalog.domain.events.CampaignPaused;
import com.geopslabs.geops.catalog.domain.events.OfferPublished;
import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.CampaignStatus;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.exceptions.CampaignAccessDeniedException;
import com.geopslabs.geops.catalog.domain.models.exceptions.CampaignNotFoundException;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidCampaignStatusException;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OutboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Transactional
public class CampaignStatusCommandService implements ChangeCampaignStatusUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignStatusCommandService.class);
    private static final boolean REACTIVATED = true;

    private final CampaignRepositoryPort campaignRepository;
    private final OfferRepositoryPort offerRepository;
    private final MerchantStandingRepositoryPort merchantStandingRepository;
    private final OutboxPort outbox;
    private final CellInvalidator cellInvalidator;
    private final Clock clock;

    public CampaignStatusCommandService(CampaignRepositoryPort campaignRepository,
                                        OfferRepositoryPort offerRepository,
                                        MerchantStandingRepositoryPort merchantStandingRepository, OutboxPort outbox,
                                        CellInvalidator cellInvalidator, Clock clock) {
        this.campaignRepository = campaignRepository;
        this.offerRepository = offerRepository;
        this.merchantStandingRepository = merchantStandingRepository;
        this.outbox = outbox;
        this.cellInvalidator = cellInvalidator;
        this.clock = clock;
    }

    @Override
    public Campaign changeStatus(ChangeCampaignStatusCommand command) {
        var current = ownCampaign(command);
        var changed = moveTo(current, command.status());
        if (changed == current) {
            return current;
        }
        var saved = campaignRepository.save(changed);
        var offers = offerRepository.findByCampaignId(saved.getId());
        announce(saved, offers);
        cellInvalidator.evictCellsOf(offers.stream().map(Offer::getLocation).toList());
        LOGGER.info("campaign.status.changed campaignId={} businessId={} status={}", saved.getId(),
                saved.getBusinessId(), saved.getStatus());
        return saved;
    }

    private Campaign ownCampaign(ChangeCampaignStatusCommand command) {
        var campaign = campaignRepository.findById(command.campaignId())
                .orElseThrow(() -> new CampaignNotFoundException(command.campaignId()));
        if (!campaign.isOfBusiness(command.businessId())) {
            throw new CampaignAccessDeniedException(command.campaignId());
        }
        return campaign;
    }

    private Campaign moveTo(Campaign campaign, CampaignStatus target) {
        return switch (target) {
            case PAUSED -> campaign.pause(today());
            case ACTIVE -> campaign.reactivate(today());
            default -> throw new InvalidCampaignStatusException();
        };
    }

    private void announce(Campaign campaign, List<Offer> offers) {
        var now = clock.instant();
        if (campaign.isActive()) {
            announceReactivation(campaign, offers, now);
            return;
        }
        var offerIds = offers.stream().map(Offer::getId).toList();
        outbox.append(CampaignPaused.of(campaign, offerIds, UUID.randomUUID(), now));
    }

    private void announceReactivation(Campaign campaign, List<Offer> offers, Instant now) {
        var businessName = businessNameOf(campaign);
        var today = today();
        offers.stream().filter(offer -> offer.isValidOn(today)).forEach(offer -> outbox.append(
                OfferPublished.of(campaign, offer, businessName, REACTIVATED, UUID.randomUUID(), now)));
    }

    private String businessNameOf(Campaign campaign) {
        return merchantStandingRepository.findByBusinessId(campaign.getBusinessId())
                .map(MerchantStanding::getBusinessName)
                .orElseThrow(() -> new IllegalStateException(
                        "Business " + campaign.getBusinessId() + " has campaigns but no local copy"));
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(OfferQueryService.OFFER_ZONE));
    }
}
