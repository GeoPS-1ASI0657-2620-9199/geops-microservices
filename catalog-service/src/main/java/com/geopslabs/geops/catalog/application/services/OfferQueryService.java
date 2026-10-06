package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.queries.GetAllOffersByCampaignIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetAllOffersQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetOfferByIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetOffersByIdsQuery;
import com.geopslabs.geops.catalog.application.usecases.OfferQueryUseCase;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
public class OfferQueryService implements OfferQueryUseCase {

    private final OfferRepositoryPort offerRepository;
    private final CampaignRepositoryPort campaignRepository;

    public OfferQueryService(OfferRepositoryPort offerRepository, CampaignRepositoryPort campaignRepository) {
        this.offerRepository = offerRepository;
        this.campaignRepository = campaignRepository;
    }

    @Override
    public List<Offer> handle(GetAllOffersQuery query) {
        try {
            return offerRepository.findAll();
        } catch (Exception e) {
            System.err.println("Error retrieving all offers: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<Offer> handle(GetOfferByIdQuery query) {
        try {
            return offerRepository.findById(query.id());
        } catch (Exception e) {
            System.err.println("Error retrieving offer by ID: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<Offer> handle(GetOffersByIdsQuery query) {
        try {
            return offerRepository.findByIdIn(query.ids());
        } catch (Exception e) {
            System.err.println("Error retrieving offers by IDs: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<Offer> handle(GetAllOffersByCampaignIdQuery query) {
        try{
            var existingCampaign = campaignRepository.findById(query.campaignId());
            if (existingCampaign.isEmpty())
                throw new IllegalArgumentException("Campaign with id " + query.campaignId() + " does not exist");
            return offerRepository.findByCampaignId(query.campaignId());
        }
        catch (Exception e){
            System.err.println("Error retrieving all offers by campaign id: " + e.getMessage());
            return List.of();
        }
    }
}
