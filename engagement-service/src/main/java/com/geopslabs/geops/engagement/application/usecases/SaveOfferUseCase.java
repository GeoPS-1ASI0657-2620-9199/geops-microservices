package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.commands.SaveOfferCommand;

public interface SaveOfferUseCase {
    SaveOfferResult save(SaveOfferCommand command);
}
