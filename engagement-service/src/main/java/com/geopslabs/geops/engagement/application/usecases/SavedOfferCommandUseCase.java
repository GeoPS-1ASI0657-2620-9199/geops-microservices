package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.models.commands.SaveOfferCommand;
import com.geopslabs.geops.engagement.domain.models.commands.RemoveSavedOfferCommand;

import java.util.Optional;

public interface SavedOfferCommandUseCase {
    Optional<SavedOffer> handle(SaveOfferCommand command);

    boolean handleDelete(Long id);

    boolean handleDelete(RemoveSavedOfferCommand command);
}
