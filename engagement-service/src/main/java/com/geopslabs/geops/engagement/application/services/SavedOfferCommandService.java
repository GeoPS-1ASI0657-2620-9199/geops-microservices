package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.application.usecases.SavedOfferCommandUseCase;
import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.models.commands.RemoveSavedOfferCommand;
import com.geopslabs.geops.engagement.domain.models.commands.SaveOfferCommand;
import com.geopslabs.geops.engagement.domain.models.exceptions.SavedOfferNotFoundException;
import com.geopslabs.geops.engagement.domain.ports.SavedOfferRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SavedOfferCommandService implements SavedOfferCommandUseCase {
    private static final long NOTHING_REMOVED = 0L;

    private final SavedOfferRepositoryPort savedOfferRepository;

    public SavedOfferCommandService(SavedOfferRepositoryPort savedOfferRepository) {
        this.savedOfferRepository = savedOfferRepository;
    }

    @Override
    public SavedOffer handle(SaveOfferCommand command) {
        return savedOfferRepository.findByConsumerIdAndOfferId(command.consumerId(), command.offerId())
                .orElseGet(() -> savedOfferRepository.save(new SavedOffer(command)));
    }

    @Override
    public void handle(RemoveSavedOfferCommand command) {
        var removed = savedOfferRepository.deleteByConsumerIdAndOfferId(command.consumerId(), command.offerId());
        if (removed == NOTHING_REMOVED) {
            throw new SavedOfferNotFoundException();
        }
    }
}
