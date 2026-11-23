package com.geopslabs.geops.catalog.domain.models;

import java.util.EnumSet;
import java.util.Set;

/**
 * Filter of the moderation panel. No status means every status; reportedOnly keeps the offers whose business has
 * open reports; text matches the title or the business name, ignoring case.
 */
public record ModerationFilter(Set<OfferStatus> statuses, boolean reportedOnly, String text) {
    public ModerationFilter {
        statuses = statuses == null || statuses.isEmpty() ? EnumSet.allOf(OfferStatus.class) : Set.copyOf(statuses);
        text = text == null || text.isBlank() ? null : text.strip();
    }
}
