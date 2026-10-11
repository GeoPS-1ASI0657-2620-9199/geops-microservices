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
import com.geopslabs.geops.identity.domain.models.TaxpayerRegistryEmptyException;
import com.geopslabs.geops.identity.domain.models.VerificationStatus;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.GeocodingPort;
import com.geopslabs.geops.identity.domain.ports.OutboxPort;
import com.geopslabs.geops.identity.domain.ports.RucVerificationRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.TaxpayerRegistryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Verifies the business RUC (US38, BRD-09 RF-01). Each attempt is stored; the profile, the attempt and, when the
 * seal is granted, confirmed or lost, the BusinessVerified event are written in the same transaction, so Catalog
 * never hears about a change that was not saved.
 */
@Transactional
public class BusinessVerificationService implements VerifyBusinessRucUseCase, RetryPendingVerificationsUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessVerificationService.class);

    private final BusinessProfileRepositoryPort businessProfiles;
    private final RucVerificationRepositoryPort verifications;
    private final TaxpayerRegistryPort taxpayerRegistry;
    private final GeocodingPort geocoding;
    private final OutboxPort outbox;
    private final Clock clock;
    private final Supplier<UUID> eventIds;

    public BusinessVerificationService(BusinessProfileRepositoryPort businessProfiles,
                                       RucVerificationRepositoryPort verifications,
                                       TaxpayerRegistryPort taxpayerRegistry, GeocodingPort geocoding,
                                       OutboxPort outbox, Clock clock, Supplier<UUID> eventIds) {
        this.businessProfiles = businessProfiles;
        this.verifications = verifications;
        this.taxpayerRegistry = taxpayerRegistry;
        this.geocoding = geocoding;
        this.outbox = outbox;
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
        if (command.correctedAddress() != null) {
            profile.correctAddress(command.correctedAddress());
        }
        return verifyAndRecord(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> pendingBusinessIds() {
        return businessProfiles.findIdsByVerificationStatus(VerificationStatus.PENDING);
    }

    @Override
    public VerificationOutcome retry(Long businessId) {
        var profile = businessProfiles.findById(businessId).orElseThrow(BusinessProfileNotFoundException::new);
        ensureRegistryIsLoaded();
        return verifyAndRecord(profile);
    }

    private VerificationOutcome verifyAndRecord(BusinessProfile profile) {
        var address = profile.getAddress();
        var attempt = RucVerification.evaluate(profile.getId(), profile.getRuc(),
                profile.getRuc().isWellFormed() ? taxpayerRegistry.findByRuc(profile.getRuc()) : Optional.empty(),
                () -> geocoding.geocode(address),
                clock.instant().truncatedTo(ChronoUnit.MILLIS));
        var verification = verifications.save(attempt);
        var sealChange = profile.applyVerification(verification);
        businessProfiles.save(profile);
        if (sealChange.mustBePublished()) {
            outbox.append(BusinessVerified.of(profile, verification, eventIds.get()));
        }
        LOGGER.info("business.verification.recorded businessId={} result={} reason={} status={} seal={}",
                profile.getId(), verification.result(), verification.reason(), profile.getVerificationStatus(),
                sealChange);
        return new VerificationOutcome(verification, profile.getVerificationStatus(), address);
    }

    private void ensureRegistryIsLoaded() {
        if (!taxpayerRegistry.isLoaded()) {
            LOGGER.warn("business.verification.unavailable reason=taxpayer-registry-empty");
            throw new TaxpayerRegistryEmptyException();
        }
    }
}
