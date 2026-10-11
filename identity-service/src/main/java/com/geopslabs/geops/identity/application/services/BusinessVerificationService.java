package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.RetryPendingVerificationsUseCase;
import com.geopslabs.geops.identity.application.usecases.VerificationOutcome;
import com.geopslabs.geops.identity.application.usecases.VerifyBusinessRucCommand;
import com.geopslabs.geops.identity.application.usecases.VerifyBusinessRucUseCase;
import com.geopslabs.geops.identity.domain.events.BusinessVerified;
import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.models.BusinessProfileAccessDeniedException;
import com.geopslabs.geops.identity.domain.models.BusinessProfileNotFoundException;
import com.geopslabs.geops.identity.domain.models.RucVerification;
import com.geopslabs.geops.identity.domain.models.Taxpayer;
import com.geopslabs.geops.identity.domain.models.TaxpayerRegistryEmptyException;
import com.geopslabs.geops.identity.domain.models.VerificationStatus;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.GeocodingPort;
import com.geopslabs.geops.identity.domain.ports.OutboxPort;
import com.geopslabs.geops.identity.domain.ports.RucVerificationRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.TaxpayerRegistryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Verifies the business RUC (US38, BRD-09 RF-01). The profile and the taxpayer are read and the address is geocoded
 * outside any transaction, so a slow Nominatim (up to 2 s, plus the 1 s spacing between requests) never holds a
 * database connection. Then a short transaction reads the profile again and writes the attempt, the profile and,
 * when the seal is granted, confirmed or lost, the BusinessVerified event, so Catalog never hears about a change
 * that was not saved.
 */
public class BusinessVerificationService implements VerifyBusinessRucUseCase, RetryPendingVerificationsUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessVerificationService.class);

    private final BusinessProfileRepositoryPort businessProfiles;
    private final RucVerificationRepositoryPort verifications;
    private final TaxpayerRegistryPort taxpayerRegistry;
    private final GeocodingPort geocoding;
    private final OutboxPort outbox;
    private final TransactionOperations transactions;
    private final Clock clock;
    private final Supplier<UUID> eventIds;

    public BusinessVerificationService(BusinessProfileRepositoryPort businessProfiles,
                                       RucVerificationRepositoryPort verifications,
                                       TaxpayerRegistryPort taxpayerRegistry, GeocodingPort geocoding,
                                       OutboxPort outbox, TransactionOperations transactions, Clock clock,
                                       Supplier<UUID> eventIds) {
        this.businessProfiles = businessProfiles;
        this.verifications = verifications;
        this.taxpayerRegistry = taxpayerRegistry;
        this.geocoding = geocoding;
        this.outbox = outbox;
        this.transactions = transactions;
        this.clock = clock;
        this.eventIds = eventIds;
    }

    @Override
    public VerificationOutcome verify(VerifyBusinessRucCommand command) {
        if (!command.businessId().equals(command.requesterBusinessId())) {
            LOGGER.info("business.verification.rejected reason=not-the-owner businessId={}", command.businessId());
            throw new BusinessProfileAccessDeniedException();
        }
        var profile = businessProfiles.findById(command.businessId())
                .orElseThrow(BusinessProfileNotFoundException::new);
        ensureRegistryIsLoaded();
        var correctedAddress = command.correctedAddress() == null
                ? null : BusinessProfile.normalizedAddress(command.correctedAddress());
        return verifyAndRecord(profile, correctedAddress);
    }

    @Override
    public List<Long> pendingBusinessIds() {
        return businessProfiles.findIdsByVerificationStatus(VerificationStatus.PENDING);
    }

    @Override
    public VerificationOutcome retry(Long businessId) {
        var profile = businessProfiles.findById(businessId).orElseThrow(BusinessProfileNotFoundException::new);
        ensureRegistryIsLoaded();
        return verifyAndRecord(profile, null);
    }

    private VerificationOutcome verifyAndRecord(BusinessProfile profile, String correctedAddress) {
        var address = correctedAddress != null ? correctedAddress : profile.getAddress();
        var ruc = profile.getRuc();
        Optional<Taxpayer> taxpayer = ruc.isWellFormed() ? taxpayerRegistry.findByRuc(ruc) : Optional.empty();
        var attempt = RucVerification.evaluate(profile.getId(), ruc, taxpayer, () -> geocoding.geocode(address),
                clock.instant().truncatedTo(ChronoUnit.MILLIS));
        return transactions.execute(status -> record(profile.getId(), attempt, correctedAddress, address));
    }

    private VerificationOutcome record(Long businessId, RucVerification attempt, String correctedAddress,
                                       String checkedAddress) {
        // Locked until commit: a simultaneous verification of the same business (double click, two devices or the
        // pending retry) waits here and applies its result on the state this one saved, so the status and the
        // last BusinessVerified always agree.
        var current = businessProfiles.findByIdForUpdate(businessId)
                .orElseThrow(BusinessProfileNotFoundException::new);
        var verification = verifications.save(attempt);
        var sealChange = current.applyVerification(verification, correctedAddress);
        businessProfiles.save(current);
        if (sealChange.mustBePublished()) {
            outbox.append(BusinessVerified.of(current, verification, eventIds.get()));
        }
        LOGGER.info("business.verification.recorded businessId={} result={} reason={} status={} seal={}",
                businessId, verification.result(), verification.reason(), current.getVerificationStatus(),
                sealChange);
        return new VerificationOutcome(verification, current.getVerificationStatus(), checkedAddress);
    }

    private void ensureRegistryIsLoaded() {
        if (!taxpayerRegistry.isLoaded()) {
            LOGGER.warn("business.verification.unavailable reason=taxpayer-registry-empty");
            throw new TaxpayerRegistryEmptyException();
        }
    }
}
