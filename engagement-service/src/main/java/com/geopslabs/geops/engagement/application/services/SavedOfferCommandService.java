package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.models.commands.SaveOfferCommand;
import com.geopslabs.geops.engagement.domain.models.commands.RemoveSavedOfferCommand;
import com.geopslabs.geops.engagement.application.usecases.SavedOfferCommandUseCase;
import com.geopslabs.geops.engagement.domain.ports.SavedOfferRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
public class SavedOfferCommandService implements SavedOfferCommandUseCase {
    private final SavedOfferRepositoryPort savedOfferRepository;

    public SavedOfferCommandService(SavedOfferRepositoryPort savedOfferRepository) {
        this.savedOfferRepository = savedOfferRepository;
    }

    @Override
    public Optional<SavedOffer> handle(SaveOfferCommand command) {
        try {
            boolean exists = savedOfferRepository.existsByConsumerIdAndOfferId(
                    command.consumerId(),
                    command.offerId()
            );

            if (exists) {
                System.err.println("SavedOffer already exists for consumerId: " +
                        command.consumerId() + " and offerId: " + command.offerId());
                return Optional.empty();
            }

            var savedOffer = new SavedOffer(command);

            var savedSavedOffer = savedOfferRepository.save(savedOffer);

            return Optional.of(savedSavedOffer);

        } catch (Exception e) {
            System.err.println("Error creating savedOffer: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public boolean handleDelete(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id cannot be null or negative");
        }

        try {
            if (!savedOfferRepository.existsById(id)) {
                return false;
            }

            savedOfferRepository.deleteById(id);
            return true;

        } catch (Exception e) {
            System.err.println("Error deleting savedOffer: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean handleDelete(RemoveSavedOfferCommand command) {
        try {
            boolean exists = savedOfferRepository.existsByConsumerIdAndOfferId(
                    command.consumerId(),
                    command.offerId()
            );

            if (!exists) {
                System.err.println("SavedOffer not found for consumerId: " +
                        command.consumerId() + " and offerId: " + command.offerId());
                return false;
            }

            long deletedCount = savedOfferRepository.deleteByConsumerIdAndOfferId(
                    command.consumerId(),
                    command.offerId()
            );

            return deletedCount > 0;

        } catch (Exception e) {
            System.err.println("Error deleting savedOffer by consumerId and offerId: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
