package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.ModerationEntry;
import com.geopslabs.geops.catalog.domain.models.ResultPage;

/** US24: who removed which offer, when and why. */
public interface ListModerationHistoryUseCase {
    ResultPage<ModerationEntry> listHistory(int page, int size);
}
