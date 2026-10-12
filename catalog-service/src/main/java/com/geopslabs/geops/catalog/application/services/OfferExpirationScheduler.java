package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.domain.ports.JobLockPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Programador de Vencimientos (Informe 4.3.4). Marks EXPIRED the published offers whose validity ended before today
 * in Lima and deletes their cells after the commit. Each round takes an advisory lock, so with two replicas only one
 * of them runs it. Reading a cell already drops the offers that expired, so the search is right even between rounds.
 */
public class OfferExpirationScheduler {
    static final String LOCK = "catalog.offer-expiration";
    private static final Logger LOGGER = LoggerFactory.getLogger(OfferExpirationScheduler.class);

    private final OfferRepositoryPort offers;
    private final JobLockPort jobLock;
    private final CellInvalidator cellInvalidator;
    private final TransactionOperations transactions;
    private final Clock clock;

    public OfferExpirationScheduler(OfferRepositoryPort offers, JobLockPort jobLock, CellInvalidator cellInvalidator,
                                    TransactionOperations transactions, Clock clock) {
        this.offers = offers;
        this.jobLock = jobLock;
        this.cellInvalidator = cellInvalidator;
        this.transactions = transactions;
        this.clock = clock;
    }

    /** Number of offers with coordinates that expired in this round; -1 when another replica holds the lock. */
    public int expireOffers() {
        var expired = transactions.execute(status -> {
            if (!jobLock.tryLock(LOCK)) {
                return -1;
            }
            var locations = offers.expirePublishedBefore(LocalDate.now(clock));
            cellInvalidator.evictCellsOf(locations);
            return locations.size();
        });
        var count = expired == null ? 0 : expired;
        if (count < 0) {
            LOGGER.debug("offers.expiration.skipped reason=another-replica-holds-the-lock");
        } else if (count > 0) {
            LOGGER.info("offers.expired count={}", count);
        }
        return count;
    }
}
