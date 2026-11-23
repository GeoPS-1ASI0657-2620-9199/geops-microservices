package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.ListModerationHistoryUseCase;
import com.geopslabs.geops.catalog.application.usecases.ListOffersForModerationUseCase;
import com.geopslabs.geops.catalog.domain.models.ModerationCandidate;
import com.geopslabs.geops.catalog.domain.models.ModerationEntry;
import com.geopslabs.geops.catalog.domain.models.ModerationFilter;
import com.geopslabs.geops.catalog.domain.models.ResultPage;
import com.geopslabs.geops.catalog.domain.ports.ModerationRepositoryPort;

public class ModerationQueryService implements ListOffersForModerationUseCase, ListModerationHistoryUseCase {
    public static final int MAX_PAGE_SIZE = 50;
    private static final int FIRST_PAGE = 0;
    private static final int MIN_PAGE_SIZE = 1;

    private final ModerationRepositoryPort moderation;

    public ModerationQueryService(ModerationRepositoryPort moderation) {
        this.moderation = moderation;
    }

    @Override
    public ResultPage<ModerationCandidate> listOffers(ModerationFilter filter, int page, int size) {
        requireValidPage(page, size);
        return moderation.findOffers(filter, page, size);
    }

    @Override
    public ResultPage<ModerationEntry> listHistory(int page, int size) {
        requireValidPage(page, size);
        return moderation.findHistory(page, size);
    }

    private static void requireValidPage(int page, int size) {
        if (page < FIRST_PAGE || size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new InvalidPageRequestException(MAX_PAGE_SIZE);
        }
    }
}
