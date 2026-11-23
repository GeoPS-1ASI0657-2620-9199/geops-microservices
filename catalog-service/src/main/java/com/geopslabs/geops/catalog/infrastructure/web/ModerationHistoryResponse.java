package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.ModerationEntry;
import com.geopslabs.geops.catalog.domain.models.ResultPage;

import java.time.Instant;
import java.util.List;

public record ModerationHistoryResponse(List<Item> content, int page, long totalElements, int totalPages) {

    static ModerationHistoryResponse from(ResultPage<ModerationEntry> page) {
        var items = page.content().stream().map(entry -> new Item(entry.moderationId(), entry.offerId(),
                entry.offerTitle(), entry.adminId(), entry.reason(), entry.removedAt())).toList();
        return new ModerationHistoryResponse(items, page.page(), page.totalElements(), page.totalPages());
    }

    public record Item(Long moderationId, Long offerId, String offerTitle, Long adminId, String reason,
                       Instant removedAt) {
    }
}
