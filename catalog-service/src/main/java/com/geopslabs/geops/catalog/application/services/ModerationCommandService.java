package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.RemoveOfferCommand;
import com.geopslabs.geops.catalog.application.usecases.RemoveOfferUseCase;
import com.geopslabs.geops.catalog.domain.events.OfferRemoved;
import com.geopslabs.geops.catalog.domain.models.ModerationAction;
import com.geopslabs.geops.catalog.domain.models.exceptions.OfferAlreadyRemovedException;
import com.geopslabs.geops.catalog.domain.models.exceptions.OfferNotFoundException;
import com.geopslabs.geops.catalog.domain.ports.ModerationRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OutboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Removes an offer in one transaction (US24): the offer goes to REMOVED, the moderation action is recorded, the
 * OfferRemoved event of a campaign offer goes to the outbox, and after the commit its cached cell is deleted so the
 * next search no longer shows it.
 */
@Transactional
public class ModerationCommandService implements RemoveOfferUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationCommandService.class);

    private final OfferRepositoryPort offers;
    private final ModerationRepositoryPort moderation;
    private final OutboxPort outbox;
    private final CellInvalidator cellInvalidator;
    private final Clock clock;

    public ModerationCommandService(OfferRepositoryPort offers, ModerationRepositoryPort moderation,
                                    OutboxPort outbox, CellInvalidator cellInvalidator, Clock clock) {
        this.offers = offers;
        this.moderation = moderation;
        this.outbox = outbox;
        this.cellInvalidator = cellInvalidator;
        this.clock = clock;
    }

    @Override
    public ModerationAction remove(RemoveOfferCommand command) {
        var offer = offers.findById(command.offerId())
                .orElseThrow(() -> new OfferNotFoundException(command.offerId()));
        var action = offer.remove(command.adminId(), command.reason(), clock.instant());
        if (!offers.markRemoved(offer.getId())) {
            throw new OfferAlreadyRemovedException(offer.getId());
        }
        var saved = moderation.save(action);
        if (offer.getCampaignId() != null) {
            outbox.append(OfferRemoved.of(offer, saved, UUID.randomUUID()));
        }
        cellInvalidator.evictCellsOf(offer.getLocation() == null ? List.of() : List.of(offer.getLocation()));
        LOGGER.info("offer.removed offerId={} adminId={} moderationId={}", offer.getId(), saved.adminId(),
                saved.id());
        return saved;
    }
}
