package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.CreateDetailsConsumerCommand;
import com.geopslabs.geops.identity.application.usecases.DetailsConsumerCommandUseCase;
import com.geopslabs.geops.identity.application.usecases.UpdateDetailsConsumerCommand;
import com.geopslabs.geops.identity.domain.models.ConsumerProfile;
import com.geopslabs.geops.identity.domain.ports.ConsumerProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
public class DetailsConsumerCommandService implements DetailsConsumerCommandUseCase {
    private final ConsumerProfileRepositoryPort consumerProfileRepository;
    private final UserRepositoryPort userRepository;

    public DetailsConsumerCommandService(ConsumerProfileRepositoryPort consumerProfileRepository,
                                         UserRepositoryPort userRepository) {
        this.consumerProfileRepository = consumerProfileRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<ConsumerProfile> handle(CreateDetailsConsumerCommand command) {
        if (consumerProfileRepository.existsByUserId(command.userId())) {
            return Optional.empty();
        }
        if (!userRepository.existsById(command.userId())) {
            return Optional.empty();
        }
        var consumerProfile = new ConsumerProfile(command.userId(), command.locationPermission(),
                command.searchRadiusMinutes(), command.defaultDistrict());
        return Optional.of(consumerProfileRepository.save(consumerProfile));
    }

    @Override
    public Optional<ConsumerProfile> handle(UpdateDetailsConsumerCommand command) {
        return consumerProfileRepository.findByUserId(command.userId())
                .map(profile -> update(profile, command))
                .map(consumerProfileRepository::save);
    }

    private ConsumerProfile update(ConsumerProfile profile, UpdateDetailsConsumerCommand command) {
        profile.updateConsumerDetails(command.locationPermission(), command.searchRadiusMinutes(),
                command.defaultDistrict());
        return profile;
    }
}
