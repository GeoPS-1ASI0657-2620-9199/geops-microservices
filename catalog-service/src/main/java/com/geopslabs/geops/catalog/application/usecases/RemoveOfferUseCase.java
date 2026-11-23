package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.ModerationAction;

/**
 * US24: the administrator removes an offer that breaks the publication rules, giving the reason.
 */
public interface RemoveOfferUseCase {
    ModerationAction remove(RemoveOfferCommand command);
}
