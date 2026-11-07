package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.exceptions.MissingLocalCopyException;
import com.geopslabs.geops.catalog.application.usecases.ApplyComplianceEventUseCase;
import com.geopslabs.geops.catalog.application.usecases.ComplianceEvent;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.ReputationEventsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;

/**
 * Applies the Reservation events that change the compliance of a business (BRD-13, RF-03), idempotent by eventId.
 * In one transaction: record the eventId in processed_events (a redelivery stops here), add the change to the
 * standing, recalculate the index and the seal and write standing_changes if the seal changed. After the commit,
 * if the open reports or the seal changed, the cells of the business are deleted so the next search ranks it again
 * (QAS08); the caller acknowledges the event to Kafka only after this method returns.
 */
public class MerchantComplianceService implements ApplyComplianceEventUseCase {
    static final String COMPLIANCE_BELOW_THRESHOLD = "COMPLIANCE_BELOW_THRESHOLD";
    static final String COMPLIANCE_RESTORED = "COMPLIANCE_RESTORED";
    private static final String STANDING_COPY = "merchant_standing";
    private static final Logger LOGGER = LoggerFactory.getLogger(MerchantComplianceService.class);

    private final MerchantStandingRepositoryPort standings;
    private final ReputationEventsPort reputationEvents;
    private final CellInvalidator cellInvalidator;
    private final TransactionOperations transactions;
    private final Clock clock;

    public MerchantComplianceService(MerchantStandingRepositoryPort standings, ReputationEventsPort reputationEvents,
                                     CellInvalidator cellInvalidator, TransactionOperations transactions,
                                     Clock clock) {
        this.standings = standings;
        this.reputationEvents = reputationEvents;
        this.cellInvalidator = cellInvalidator;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Outcome apply(ComplianceEvent event) {
        var change = transactions.execute(status -> applyInTransaction(event));
        if (change == null) {
            LOGGER.info("merchant.standing.compliance eventId={} eventType={} businessId={} outcome={}",
                    event.eventId(), event.eventType(), event.businessId(), Outcome.DUPLICATE);
            return Outcome.DUPLICATE;
        }
        if (change.updated().ranksDifferentlyFrom(change.previous())) {
            cellInvalidator.evictCellsOfBusiness(event.businessId());
        }
        var updated = change.updated();
        LOGGER.info("merchant.standing.compliance eventId={} eventType={} businessId={} openReports={} "
                        + "complianceIndex={} seal={} outcome={}", event.eventId(), event.eventType(),
                event.businessId(), updated.getOpenReports(), updated.getComplianceIndex(), updated.hasVerifiedSeal(),
                Outcome.APPLIED);
        return Outcome.APPLIED;
    }

    private StandingChange applyInTransaction(ComplianceEvent event) {
        if (!reputationEvents.markProcessed(event.eventId(), event.eventType(), clock.instant())) {
            return null;
        }
        var previous = standings.findByBusinessId(event.businessId())
                .orElseThrow(() -> new MissingLocalCopyException(STANDING_COPY, event.businessId()));
        var updated = standings.save(previous.applyCompliance(event.change()));
        if (previous.hasVerifiedSeal() != updated.hasVerifiedSeal()) {
            reputationEvents.recordSealChange(event.businessId(), updated.hasVerifiedSeal(),
                    updated.hasVerifiedSeal() ? COMPLIANCE_RESTORED : COMPLIANCE_BELOW_THRESHOLD, event.occurredAt());
        }
        return new StandingChange(previous, updated);
    }

    private record StandingChange(MerchantStanding previous, MerchantStanding updated) {
    }
}
