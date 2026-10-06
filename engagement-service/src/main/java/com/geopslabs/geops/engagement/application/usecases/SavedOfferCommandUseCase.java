package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.models.commands.RemoveSavedOfferCommand;
import com.geopslabs.geops.engagement.domain.models.commands.SaveOfferCommand;

public interface SavedOfferCommandUseCase {
    SavedOffer handle(SaveOfferCommand command);

    void handle(RemoveSavedOfferCommand command);
}
