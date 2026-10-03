package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.CreateDetailsOwnerCommand;
import com.geopslabs.geops.identity.application.usecases.DetailsOwnerCommandUseCase;
import com.geopslabs.geops.identity.application.usecases.UpdateDetailsOwnerCommand;
import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
public class DetailsOwnerCommandService implements DetailsOwnerCommandUseCase {
    private final BusinessProfileRepositoryPort businessProfileRepository;
    private final UserRepositoryPort userRepository;

    public DetailsOwnerCommandService(BusinessProfileRepositoryPort businessProfileRepository,
                                      UserRepositoryPort userRepository) {
        this.businessProfileRepository = businessProfileRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<BusinessProfile> handle(CreateDetailsOwnerCommand command) {
        if (businessProfileRepository.existsByUserId(command.userId())) {
            return Optional.empty();
        }
        return userRepository.findById(command.userId())
                .map(user -> new BusinessProfile(user, command.businessName(), command.businessType(),
                        command.taxId(), command.website(), command.description(), command.address(),
                        command.horarioAtencion()))
                .map(businessProfileRepository::save);
    }

    @Override
    public Optional<BusinessProfile> handle(UpdateDetailsOwnerCommand command) {
        return businessProfileRepository.findByUserId(command.userId())
                .map(profile -> update(profile, command))
                .map(businessProfileRepository::save);
    }

    private BusinessProfile update(BusinessProfile profile, UpdateDetailsOwnerCommand command) {
        profile.updateOwnerDetails(command.businessName(), command.businessType(), command.taxId(),
                command.website(), command.description(), command.address(), command.horarioAtencion());
        return profile;
    }
}
