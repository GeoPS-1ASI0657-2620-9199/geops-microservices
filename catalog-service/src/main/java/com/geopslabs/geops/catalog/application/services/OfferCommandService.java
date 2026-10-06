package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.domain.models.ECampaignStatus;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.commands.CreateOfferCommand;
import com.geopslabs.geops.catalog.domain.models.commands.DeleteOfferCommand;
import com.geopslabs.geops.catalog.domain.models.commands.UpdateOfferCommand;
import com.geopslabs.geops.catalog.application.usecases.OfferCommandUseCase;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
public class OfferCommandService implements OfferCommandUseCase {

    private final OfferRepositoryPort offerRepository;
    private final CampaignRepositoryPort campaignRepository;

    public OfferCommandService(OfferRepositoryPort offerRepository, CampaignRepositoryPort campaignRepository) {
        this.offerRepository = offerRepository;
        this.campaignRepository = campaignRepository;
    }

    @Override
    public Optional<Offer> handle(CreateOfferCommand command) {
        try {
            var existingCampaign = campaignRepository.findById(command.campaignId());
            if (existingCampaign.isEmpty())
                throw new IllegalArgumentException("Campaign with id " + command.campaignId() + " does not exist");

            if(existingCampaign.get().getStatus() != ECampaignStatus.ACTIVE)
                throw new IllegalArgumentException("The Campaign is not ACTIVE");

            var offer = new Offer(existingCampaign.get(), command);

            var savedOffer = offerRepository.save(offer);

            return Optional.of(savedOffer);

        } catch (Exception e) {
            System.err.println("Error creating offer: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public Optional<Offer> handle(UpdateOfferCommand command) {
        try {
            var existingOfferOpt = offerRepository.findById(command.id());

            if (existingOfferOpt.isEmpty()) {
                return Optional.empty();
            }

            var existingOffer = existingOfferOpt.get();

            existingOffer.updateOffer(command);

            var updatedOffer = offerRepository.save(existingOffer);

            return Optional.of(updatedOffer);

        } catch (Exception e) {
            System.err.println("Error updating offer: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public boolean handle(DeleteOfferCommand command) {
        if (command.id() == null || command.id() <= 0) {
            throw new IllegalArgumentException("id cannot be null or negative");
        }

        try {
            if (!offerRepository.existsById(command.id())) {
                return false;
            }

            offerRepository.deleteById(command.id());
            return true;

        } catch (Exception e) {
            System.err.println("Error deleting offer: " + e.getMessage());
            return false;
        }
    }
}
