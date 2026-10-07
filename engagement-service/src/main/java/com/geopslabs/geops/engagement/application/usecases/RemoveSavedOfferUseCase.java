package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.commands.RemoveSavedOfferCommand;

public interface RemoveSavedOfferUseCase {
    void remove(RemoveSavedOfferCommand command);
}
