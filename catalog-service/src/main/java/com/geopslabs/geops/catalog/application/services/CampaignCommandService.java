package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.commands.CreateCampaignCommand;
import com.geopslabs.geops.catalog.domain.models.commands.DeleteCampaignCommand;
import com.geopslabs.geops.catalog.domain.models.commands.UpdateCampaignCommand;
import com.geopslabs.geops.catalog.domain.models.ECampaignStatus;
import com.geopslabs.geops.catalog.application.usecases.CampaignCommandUseCase;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.backend.identity.infrastructure.persistence.jpa.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

@Transactional
public class CampaignCommandService implements CampaignCommandUseCase {

    private final CampaignRepositoryPort campaignRepository;
    private final UserRepository userRepository;

    public CampaignCommandService(CampaignRepositoryPort campaignRepository, UserRepository userRepository) {
        this.campaignRepository = campaignRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<Campaign> handle(CreateCampaignCommand command) {
        try{
            //Verifies if user is found by a user id
            var foundUser = userRepository.findById(command.userId());
            if(foundUser.isEmpty()) return Optional.empty();
            //Verifies if user is OWNER role
            if(!Objects.equals(foundUser.get().getRole(), "OWNER"))
                throw new IllegalArgumentException("The user does not have OWNER role");

            var campaign = new Campaign(foundUser.get(),command);

            var savedCampaign = campaignRepository.save(campaign);

            return Optional.of(savedCampaign);
        }
        catch(Exception e){
            System.out.println("Error creating a campaign: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public Optional<Campaign> handle(UpdateCampaignCommand command) {
        try{
            var foundCampaign = campaignRepository.findById(command.id());
            if(foundCampaign.isEmpty()) throw new NoSuchElementException("Campaign not found with id: " + command.id());
                foundCampaign.get().edit(command.name(), command.description(), command.startDate(), command.endDate(),
                    ECampaignStatus.valueOf(command.status()), command.estimatedBudget(), command.totalImpressions(),
                    command.totalClicks(), command.ctr());
            var editedCampaign = campaignRepository.save(foundCampaign.get());
            return Optional.of(editedCampaign);
        }
        catch(Exception e){
            System.out.println("Error editing campaign: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public boolean handle(DeleteCampaignCommand command) {
        try{
            var foundCampaign = campaignRepository.findById(command.id());
            if(foundCampaign.isEmpty()) throw new NoSuchElementException("Campaign not found with id: " + command.id());
            campaignRepository.deleteById(command.id());
            return true;
        }
        catch(Exception e){
            System.out.println("Error deleting campaign: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


}
