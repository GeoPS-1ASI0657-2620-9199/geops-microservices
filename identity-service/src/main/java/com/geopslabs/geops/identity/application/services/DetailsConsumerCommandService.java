package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.domain.models.DetailsConsumer;
import com.geopslabs.geops.identity.application.usecases.CreateDetailsConsumerCommand;
import com.geopslabs.geops.identity.application.usecases.UpdateDetailsConsumerCommand;
import com.geopslabs.geops.identity.application.usecases.DetailsConsumerCommandUseCase;
import com.geopslabs.geops.identity.infrastructure.persistence.DetailsConsumerJpaRepository;
import com.geopslabs.geops.identity.infrastructure.persistence.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * DetailsConsumerCommandService
 *
 * Implementation of the DetailsConsumerCommandUseCase that handles all command operations
 * for consumer details. This service implements the business logic for creating and updating
 * consumer details following DDD principles
 *
 * @summary Implementation of consumer details command service operations
 * @since 1.0
 * @author GeOps Labs
 */
@Service
@Transactional
public class DetailsConsumerCommandService implements DetailsConsumerCommandUseCase {

    private final DetailsConsumerJpaRepository detailsConsumerRepository;
    private final UserJpaRepository userRepository;

    /**
     * Constructor for dependency injection
     *
     * @param detailsConsumerRepository The repository for consumer details data access
     * @param userRepository The repository for user data access
     */
    public DetailsConsumerCommandService(DetailsConsumerJpaRepository detailsConsumerRepository,
                                            UserJpaRepository userRepository) {
        this.detailsConsumerRepository = detailsConsumerRepository;
        this.userRepository = userRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<DetailsConsumer> handle(CreateDetailsConsumerCommand command) {
        try {
            // Check if consumer details already exist for this user
            if (detailsConsumerRepository.existsByUserId(command.userId())) {
                System.err.println("Consumer details for user ID " + command.userId() + " already exist");
                return Optional.empty();
            }

            // Find the user
            var userOptional = userRepository.findById(command.userId());
            if (userOptional.isEmpty()) {
                System.err.println("User with ID " + command.userId() + " not found");
                return Optional.empty();
            }

            var user = userOptional.get();

            // Create new consumer details
            var detailsConsumer = new DetailsConsumer(
                user,
                command.categoriasFavoritas(),
                command.recibirNotificaciones(),
                command.permisoUbicacion(),
                command.direccionCasa(),
                command.direccionTrabajo(),
                command.direccionUniversidad()
            );

            // Save and return the consumer details
            var savedDetails = detailsConsumerRepository.save(detailsConsumer);
            return Optional.of(savedDetails);
        } catch (Exception e) {
            System.err.println("Error creating consumer details: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<DetailsConsumer> handle(UpdateDetailsConsumerCommand command) {
        try {
            // Find the consumer details by user ID
            var detailsOptional = detailsConsumerRepository.findByUserId(command.userId());

            if (detailsOptional.isEmpty()) {
                System.err.println("Consumer details for user ID " + command.userId() + " not found");
                return Optional.empty();
            }

            var detailsConsumer = detailsOptional.get();

            // Update consumer details
            detailsConsumer.updateConsumerDetails(
                command.categoriasFavoritas(),
                command.recibirNotificaciones(),
                command.permisoUbicacion(),
                command.direccionCasa(),
                command.direccionTrabajo(),
                command.direccionUniversidad()
            );

            // Save and return the updated consumer details
            var updatedDetails = detailsConsumerRepository.save(detailsConsumer);
            return Optional.of(updatedDetails);
        } catch (Exception e) {
            System.err.println("Error updating consumer details: " + e.getMessage());
            return Optional.empty();
        }
    }
}

