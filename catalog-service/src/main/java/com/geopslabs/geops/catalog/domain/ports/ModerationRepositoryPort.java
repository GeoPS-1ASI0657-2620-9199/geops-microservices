package com.geopslabs.geops.catalog.domain.ports;

import com.geopslabs.geops.catalog.domain.models.ModerationAction;
import com.geopslabs.geops.catalog.domain.models.ModerationCandidate;
import com.geopslabs.geops.catalog.domain.models.ModerationEntry;
import com.geopslabs.geops.catalog.domain.models.ModerationFilter;
import com.geopslabs.geops.catalog.domain.models.ResultPage;

public interface ModerationRepositoryPort {
    ModerationAction save(ModerationAction action);

    /** Offers for the moderation panel, newest first. */
    ResultPage<ModerationCandidate> findOffers(ModerationFilter filter, int page, int size);

    /** Moderation actions, the most recent first. */
    ResultPage<ModerationEntry> findHistory(int page, int size);
}
