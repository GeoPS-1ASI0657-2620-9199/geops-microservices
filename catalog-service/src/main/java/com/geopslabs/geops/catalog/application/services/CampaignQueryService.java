package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.GetCampaignByIdUseCase;
import com.geopslabs.geops.catalog.application.usecases.ListBusinessCampaignsUseCase;
import com.geopslabs.geops.catalog.application.usecases.ListCampaignOffersUseCase;
import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.exceptions.CampaignAccessDeniedException;
import com.geopslabs.geops.catalog.domain.models.exceptions.CampaignNotFoundException;
import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignByIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignsByBusinessIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetOffersByCampaignIdQuery;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CampaignQueryService
        implements GetCampaignByIdUseCase, ListBusinessCampaignsUseCase, ListCampaignOffersUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignQueryService.class);

    private final CampaignRepositoryPort campaignRepository;
    private final OfferRepositoryPort offerRepository;

    public CampaignQueryService(CampaignRepositoryPort campaignRepository, OfferRepositoryPort offerRepository) {
        this.campaignRepository = campaignRepository;
        this.offerRepository = offerRepository;
    }

    @Override
    public Campaign getById(GetCampaignByIdQuery query) {
        var campaign = campaignRepository.findById(query.campaignId())
                .orElseThrow(() -> new CampaignNotFoundException(query.campaignId()));
        if (!campaign.isOfBusiness(query.businessId())) {
            LOGGER.info("campaign.access-denied campaignId={} reason=other-business", campaign.getId());
            throw new CampaignAccessDeniedException(campaign.getId());
        }
        return campaign;
    }

    @Override
    public List<Campaign> list(GetCampaignsByBusinessIdQuery query) {
        return campaignRepository.findByBusinessId(query.businessId());
    }

    @Override
    public List<Offer> list(GetOffersByCampaignIdQuery query) {
        var campaign = getById(new GetCampaignByIdQuery(query.campaignId(), query.businessId()));
        return offerRepository.findByCampaignId(campaign.getId());
    }
}
