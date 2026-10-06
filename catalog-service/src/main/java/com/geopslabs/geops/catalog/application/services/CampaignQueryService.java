package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.queries.GetAllCampaignsByBusinessIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetAllCampaignsQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignByIdQuery;
import com.geopslabs.geops.catalog.application.usecases.CampaignQueryUseCase;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
public class CampaignQueryService implements CampaignQueryUseCase {

    private final CampaignRepositoryPort campaignRepository;

    public CampaignQueryService(CampaignRepositoryPort campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Override
    public Optional<Campaign> handle(GetCampaignByIdQuery query) {
        try{
            var campaign = campaignRepository.findById(query.id());
            if(campaign.isEmpty())
                throw new IllegalArgumentException("Campaign with id " + query.id() + " not found");
            return campaign;
        }
        catch (Exception e){
            System.out.println("Error creating a campaign: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public List<Campaign> handle(GetAllCampaignsQuery query) {return campaignRepository.findAll();}

    @Override
    public List<Campaign> handle(GetAllCampaignsByBusinessIdQuery query) {
        return campaignRepository.findByBusinessId(query.businessId());
    }
}
