package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.ModerationCandidate;
import com.geopslabs.geops.catalog.domain.models.ModerationFilter;
import com.geopslabs.geops.catalog.domain.models.ResultPage;

/** US24: the offers of the catalog with their status, for the moderation panel. */
public interface ListOffersForModerationUseCase {
    ResultPage<ModerationCandidate> listOffers(ModerationFilter filter, int page, int size);
}
