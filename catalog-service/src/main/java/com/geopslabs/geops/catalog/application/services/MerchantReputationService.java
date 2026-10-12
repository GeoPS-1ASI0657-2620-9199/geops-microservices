package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.ApplyBusinessVerifiedUseCase;
import com.geopslabs.geops.catalog.application.usecases.BusinessVerifiedEvent;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.ReputationEventsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;

/**
 * Applies BusinessVerified to Catalog's copy of the seal (BRD-09, RF-03), idempotent by eventId. In one transaction:
 * record the eventId in processed_events (a redelivery stops here), ignore an event older than the copy, update or
 * create the standing, recalculate the seal and write standing_changes if it changed. After the commit the cells of
 * the business are deleted, so the next search shows the new seal (QAS08); the caller acknowledges the event to
 * Kafka only after this method returns.
 */
public class MerchantReputationService implements ApplyBusinessVerifiedUseCase {
    static final String RUC_VERIFIED = "RUC_VERIFIED";
    static final String RUC_VERIFICATION_LOST = "RUC_VERIFICATION_LOST";
    private static final Logger LOGGER = LoggerFactory.getLogger(MerchantReputationService.class);

    private final MerchantStandingRepositoryPort standings;
    private final ReputationEventsPort reputationEvents;
    private final CellInvalidator cellInvalidator;
    private final TransactionOperations transactions;
    private final Clock clock;

    public MerchantReputationService(MerchantStandingRepositoryPort standings, ReputationEventsPort reputationEvents,
                                     CellInvalidator cellInvalidator, TransactionOperations transactions,
                                     Clock clock) {
        this.standings = standings;
        this.reputationEvents = reputationEvents;
        this.cellInvalidator = cellInvalidator;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Outcome apply(BusinessVerifiedEvent event) {
        var outcome = transactions.execute(status -> applyInTransaction(event));
        if (outcome == Outcome.APPLIED) {
            cellInvalidator.evictCellsOfBusiness(event.businessId());
        }
        LOGGER.info("merchant.standing.business-verified eventId={} businessId={} verified={} outcome={}",
                event.eventId(), event.businessId(), event.verified(), outcome);
        return outcome;
    }

    private Outcome applyInTransaction(BusinessVerifiedEvent event) {
        if (!reputationEvents.markProcessed(event.eventId(), BusinessVerifiedEvent.EVENT_TYPE, clock.instant())) {
            return Outcome.DUPLICATE;
        }
        var current = standings.findByBusinessId(event.businessId());
        if (current.isPresent() && current.get().isNewerThan(event.occurredAt())) {
            return Outcome.STALE;
        }
        var updated = current
                .map(standing -> standing.applyRucVerification(event.verified(), event.businessName(),
                        event.occurredAt()))
                .orElseGet(() -> MerchantStanding.fromRucVerification(event.businessId(), event.businessName(),
                        event.verified(), event.occurredAt()));
        standings.save(updated);
        var hadSeal = current.map(MerchantStanding::hasVerifiedSeal).orElse(false);
        if (hadSeal != updated.hasVerifiedSeal()) {
            reputationEvents.recordSealChange(event.businessId(), updated.hasVerifiedSeal(),
                    event.verified() ? RUC_VERIFIED : RUC_VERIFICATION_LOST, event.occurredAt());
        }
        return Outcome.APPLIED;
    }
}
