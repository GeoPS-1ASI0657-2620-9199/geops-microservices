package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.application.usecases.RemoveSavedOfferUseCase;
import com.geopslabs.geops.engagement.application.usecases.SaveOfferResult;
import com.geopslabs.geops.engagement.application.usecases.SaveOfferUseCase;
import com.geopslabs.geops.engagement.application.usecases.SavedOfferView;
import com.geopslabs.geops.engagement.domain.models.BusinessSnapshot;
import com.geopslabs.geops.engagement.domain.models.OfferSnapshot;
import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.models.commands.RemoveSavedOfferCommand;
import com.geopslabs.geops.engagement.domain.models.commands.SaveOfferCommand;
import com.geopslabs.geops.engagement.domain.models.exceptions.OfferNotAvailableException;
import com.geopslabs.geops.engagement.domain.models.exceptions.OfferNotFoundException;
import com.geopslabs.geops.engagement.domain.models.exceptions.SavedOfferAlreadyExistsException;
import com.geopslabs.geops.engagement.domain.models.exceptions.SavedOfferNotFoundException;
import com.geopslabs.geops.engagement.domain.ports.BusinessSnapshotRepositoryPort;
import com.geopslabs.geops.engagement.domain.ports.OfferSnapshotRepositoryPort;
import com.geopslabs.geops.engagement.domain.ports.SavedOfferRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class SavedOfferCommandService implements SaveOfferUseCase, RemoveSavedOfferUseCase {
    public static final ZoneId OFFER_ZONE = ZoneId.of("America/Lima");
    private static final long NOTHING_REMOVED = 0L;
    private static final Logger LOGGER = LoggerFactory.getLogger(SavedOfferCommandService.class);

    private final SavedOfferRepositoryPort savedOfferRepository;
    private final OfferSnapshotRepositoryPort offerSnapshotRepository;
    private final BusinessSnapshotRepositoryPort businessSnapshotRepository;
    private final Clock clock;

    public SavedOfferCommandService(SavedOfferRepositoryPort savedOfferRepository,
                                    OfferSnapshotRepositoryPort offerSnapshotRepository,
                                    BusinessSnapshotRepositoryPort businessSnapshotRepository, Clock clock) {
        this.savedOfferRepository = savedOfferRepository;
        this.offerSnapshotRepository = offerSnapshotRepository;
        this.businessSnapshotRepository = businessSnapshotRepository;
        this.clock = clock;
    }

    @Override
    public SaveOfferResult save(SaveOfferCommand command) {
        var offer = offerSnapshotRepository.findById(command.offerId()).orElseThrow(OfferNotFoundException::new);
        if (offer.isExpired(LocalDate.now(clock.withZone(OFFER_ZONE)))) {
            throw new OfferNotAvailableException();
        }
        var existing = savedOfferRepository.findByConsumerIdAndOfferId(command.consumerId(), command.offerId());
        if (existing.isPresent()) {
            return SaveOfferResult.existing(viewOf(existing.get(), offer));
        }
        return insert(command, offer);
    }

    @Override
    public void remove(RemoveSavedOfferCommand command) {
        var removed = savedOfferRepository.deleteByConsumerIdAndOfferId(command.consumerId(), command.offerId());
        if (removed == NOTHING_REMOVED) {
            throw new SavedOfferNotFoundException();
        }
        LOGGER.info("saved-offer.removed offerId={}", command.offerId());
    }

    private SaveOfferResult insert(SaveOfferCommand command, OfferSnapshot offer) {
        try {
            var saved = savedOfferRepository.save(SavedOffer.create(command.consumerId(), offer.offerId(), now()));
            LOGGER.info("saved-offer.created savedOfferId={} offerId={}", saved.getId(), offer.offerId());
            return SaveOfferResult.created(viewOf(saved, offer));
        } catch (SavedOfferAlreadyExistsException concurrentInsert) {
            LOGGER.info("saved-offer.reused offerId={} reason=concurrent-request", offer.offerId());
            return savedOfferRepository.findByConsumerIdAndOfferId(command.consumerId(), command.offerId())
                    .map(saved -> SaveOfferResult.existing(viewOf(saved, offer)))
                    .orElseThrow(() -> concurrentInsert);
        }
    }

    private SavedOfferView viewOf(SavedOffer savedOffer, OfferSnapshot offer) {
        var businessName = businessSnapshotRepository.findById(offer.businessId())
                .map(BusinessSnapshot::businessName)
                .orElse(null);
        return new SavedOfferView(savedOffer, offer, businessName, false);
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }
}
