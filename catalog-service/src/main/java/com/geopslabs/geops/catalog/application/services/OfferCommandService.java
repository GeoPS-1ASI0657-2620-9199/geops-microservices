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

/**
 * OfferCommandService
 * Implementation of the OfferCommandUseCase that handles all command operations
 * for offers. This service implements the business logic for
 * creating, updating, and managing offers following DDD principles
 *
 * @summary Implementation of offer command service operations
 * @since 1.0
 * @author GeOps Labs
 */
@Transactional
public class OfferCommandService implements OfferCommandUseCase {

    private final OfferRepositoryPort offerRepository;
    private final CampaignRepositoryPort campaignRepository;

    /**
     * Constructor for dependency injection
     *
     * @param offerRepository The repository for offer data access
     */
    public OfferCommandService(OfferRepositoryPort offerRepository, CampaignRepositoryPort campaignRepository) {
        this.offerRepository = offerRepository;
        this.campaignRepository = campaignRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Offer> handle(CreateOfferCommand command) {
        try {
            //Verifying the campaign exists
            var existingCampaign = campaignRepository.findById(command.campaignId());
            if (existingCampaign.isEmpty())
                throw new IllegalArgumentException("Campaign with id " + command.campaignId() + " does not exist");

            //Verifying if the campaign is active
            if(existingCampaign.get().getStatus() != ECampaignStatus.ACTIVE)
                throw new IllegalArgumentException("The Campaign is not ACTIVE");

            // Create new offer from command
            var offer = new Offer(existingCampaign.get(), command);

            // Save the offer to the repository
            var savedOffer = offerRepository.save(offer);

            return Optional.of(savedOffer);

        } catch (Exception e) {
            // Log the error with full stacktrace
            System.err.println("Error creating offer: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Offer> handle(UpdateOfferCommand command) {
        try {
            // Find the existing offer by ID
            var existingOfferOpt = offerRepository.findById(command.id());

            if (existingOfferOpt.isEmpty()) {
                return Optional.empty();
            }

            var existingOffer = existingOfferOpt.get();

            // Update the offer with new data
            existingOffer.updateOffer(command);

            // Save the updated offer (this should trigger @PreUpdate)
            var updatedOffer = offerRepository.save(existingOffer);

            return Optional.of(updatedOffer);

        } catch (Exception e) {
            // Log the error with full stacktrace
            System.err.println("Error updating offer: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean handle(DeleteOfferCommand command) {
        if (command.id() == null || command.id() <= 0) {
            throw new IllegalArgumentException("id cannot be null or negative");
        }

        try {
            // First check if offer exists
            if (!offerRepository.existsById(command.id())) {
                return false;
            }

            // Delete the offer
            offerRepository.deleteById(command.id());
            return true;

        } catch (Exception e) {
            // Log the error (in a real application, use proper logging framework)
            System.err.println("Error deleting offer: " + e.getMessage());
            return false;
        }
    }
}
