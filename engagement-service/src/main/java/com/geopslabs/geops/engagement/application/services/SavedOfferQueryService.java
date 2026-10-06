package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.application.usecases.ListSavedOffersUseCase;
import com.geopslabs.geops.engagement.application.usecases.SavedOfferView;
import com.geopslabs.geops.engagement.domain.models.BusinessSnapshot;
import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOffersByConsumerQuery;
import com.geopslabs.geops.engagement.domain.ports.BusinessSnapshotRepositoryPort;
import com.geopslabs.geops.engagement.domain.ports.OfferSnapshotRepositoryPort;
import com.geopslabs.geops.engagement.domain.ports.SavedOfferRepositoryPort;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class SavedOfferQueryService implements ListSavedOffersUseCase {
    private final SavedOfferRepositoryPort savedOfferRepository;
    private final OfferSnapshotRepositoryPort offerSnapshotRepository;
    private final BusinessSnapshotRepositoryPort businessSnapshotRepository;
    private final Clock clock;

    public SavedOfferQueryService(SavedOfferRepositoryPort savedOfferRepository,
                                  OfferSnapshotRepositoryPort offerSnapshotRepository,
                                  BusinessSnapshotRepositoryPort businessSnapshotRepository, Clock clock) {
        this.savedOfferRepository = savedOfferRepository;
        this.offerSnapshotRepository = offerSnapshotRepository;
        this.businessSnapshotRepository = businessSnapshotRepository;
        this.clock = clock;
    }

    @Override
    public List<SavedOfferView> list(GetSavedOffersByConsumerQuery query) {
        var today = LocalDate.now(clock.withZone(SavedOfferCommandService.OFFER_ZONE));
        return savedOfferRepository.findByConsumerId(query.consumerId()).stream()
                .map(savedOffer -> viewOf(savedOffer, today))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<SavedOfferView> viewOf(SavedOffer savedOffer, LocalDate today) {
        return offerSnapshotRepository.findById(savedOffer.getOfferId()).map(offer -> {
            var businessName = businessSnapshotRepository.findById(offer.businessId())
                    .map(BusinessSnapshot::businessName)
                    .orElse(null);
            return new SavedOfferView(savedOffer, offer, businessName, offer.isExpired(today));
        });
    }
}
