package com.geopslabs.geops.catalog.domain.models;

import java.time.LocalDate;

/** Impressions and clicks of one offer on one day, added up in memory and written in one batch. */
public record OfferInteractions(Long offerId, LocalDate date, long impressions, long clicks) {
}
