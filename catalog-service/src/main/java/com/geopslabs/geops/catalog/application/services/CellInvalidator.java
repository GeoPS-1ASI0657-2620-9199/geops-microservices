package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.domain.models.GeoCell;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.ports.CellCachePort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deletes the cached cells a write changed (QAS08). Inside a transaction the cells are deleted right after the
 * commit, still before the HTTP response or the Kafka acknowledgement: deleting them before the commit would let a
 * concurrent search cache the old rows again.
 */
public class CellInvalidator {
    private final CellCachePort cellCache;
    private final OfferRepositoryPort offers;
    private final Clock clock;

    public CellInvalidator(CellCachePort cellCache, OfferRepositoryPort offers, Clock clock) {
        this.cellCache = cellCache;
        this.offers = offers;
        this.clock = clock;
    }

    public void evictCellsOf(Collection<GeoPoint> locations) {
        var cells = locations.stream()
                .filter(Objects::nonNull)
                .map(location -> GeoCell.containing(location).geohash())
                .collect(Collectors.toSet());
        if (!cells.isEmpty()) {
            afterCommit(cells);
        }
    }

    /** The cells of the offers of the business that are still valid: its seal or name changed. */
    public void evictCellsOfBusiness(Long businessId) {
        evictCellsOf(offers.findPublishedLocationsOfBusiness(businessId, LocalDate.now(clock)));
    }

    private void afterCommit(Set<String> cells) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cellCache.evict(cells);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cellCache.evict(cells);
            }
        });
    }
}
