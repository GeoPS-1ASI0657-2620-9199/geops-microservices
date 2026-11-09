package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.domain.events.OfferExpired;
import com.geopslabs.geops.catalog.domain.models.ExpiredOffer;
import com.geopslabs.geops.catalog.domain.ports.JobLockPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OutboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Programador de Vencimientos (Informe 4.3.4). Marks EXPIRED the published offers whose validity ended before today
 * in Lima, writes one OfferExpired per campaign offer in the same transaction and deletes their cells. Each round
 * takes an advisory lock, so with two replicas only one of them runs it. Reading a cell already drops the offers
 * that expired, so the search is right even between rounds.
 */
public class OfferExpirationScheduler {
    static final String LOCK = "catalog.offer-expiration";
    static final int SKIPPED = -1;
    private static final Logger LOGGER = LoggerFactory.getLogger(OfferExpirationScheduler.class);

    private final OfferRepositoryPort offers;
    private final JobLockPort jobLock;
    private final OutboxPort outbox;
    private final CellInvalidator cellInvalidator;
    private final TransactionOperations transactions;
    private final Clock clock;

    public OfferExpirationScheduler(OfferRepositoryPort offers, JobLockPort jobLock, OutboxPort outbox,
                                    CellInvalidator cellInvalidator, TransactionOperations transactions, Clock clock) {
        this.offers = offers;
        this.jobLock = jobLock;
        this.outbox = outbox;
        this.cellInvalidator = cellInvalidator;
        this.transactions = transactions;
        this.clock = clock;
    }

    /** Number of offers with coordinates that expired in this round; -1 when another replica holds the lock. */
    public int expireOffers() {
        var expired = transactions.execute(status -> jobLock.tryLock(LOCK) ? expireLocked() : SKIPPED);
        var count = expired == null ? 0 : expired;
        if (count < 0) {
            LOGGER.debug("offers.expiration.skipped reason=another-replica-holds-the-lock");
        } else if (count > 0) {
            LOGGER.info("offers.expired count={}", count);
        }
        return count;
    }

    private int expireLocked() {
        var expired = offers.expirePublishedBefore(LocalDate.now(clock));
        announce(expired);
        var locations = expired.stream().filter(ExpiredOffer::hasLocation).map(ExpiredOffer::location).toList();
        cellInvalidator.evictCellsOf(locations);
        return locations.size();
    }

    private void announce(List<ExpiredOffer> expired) {
        var now = clock.instant();
        expired.stream()
                .filter(ExpiredOffer::belongsToCampaign)
                .forEach(offer -> outbox.append(OfferExpired.of(offer, UUID.randomUUID(), now)));
    }
}
