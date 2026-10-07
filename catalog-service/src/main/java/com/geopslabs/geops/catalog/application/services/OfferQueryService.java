package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.GetOfferAvailabilityUseCase;
import com.geopslabs.geops.catalog.application.usecases.GetOfferDetailUseCase;
import com.geopslabs.geops.catalog.application.usecases.OfferAvailability;
import com.geopslabs.geops.catalog.application.usecases.OfferDetail;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import com.geopslabs.geops.catalog.domain.models.exceptions.OfferNotFoundException;
import com.geopslabs.geops.catalog.domain.models.queries.GetOfferByIdQuery;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

public class OfferQueryService implements GetOfferDetailUseCase, GetOfferAvailabilityUseCase {
    public static final ZoneId OFFER_ZONE = ZoneId.of("America/Lima");

    private final OfferRepositoryPort offerRepository;
    private final MerchantStandingRepositoryPort merchantStandingRepository;
    private final Clock clock;

    public OfferQueryService(OfferRepositoryPort offerRepository,
                             MerchantStandingRepositoryPort merchantStandingRepository, Clock clock) {
        this.offerRepository = offerRepository;
        this.merchantStandingRepository = merchantStandingRepository;
        this.clock = clock;
    }

    @Override
    public OfferDetail getDetail(GetOfferByIdQuery query) {
        var offer = findOffer(query.offerId());
        if (offer.getStatus() == OfferStatus.REMOVED) {
            throw new OfferNotFoundException(offer.getId());
        }
        var available = offer.isValidOn(today());
        if (offer.getSource() == OfferSource.PUBLIC_SOURCE) {
            return new OfferDetail(offer, offer.getSourceName(), false, available);
        }
        var standing = merchantStandingRepository.findByBusinessId(offer.getBusinessId());
        return new OfferDetail(offer, standing.map(MerchantStanding::getBusinessName).orElse(null),
                standing.map(MerchantStanding::hasVerifiedSeal).orElse(false), available);
    }

    @Override
    public OfferAvailability getAvailability(GetOfferByIdQuery query) {
        var offer = findOffer(query.offerId());
        return new OfferAvailability(offer.getId(), offer.getBusinessId(), offer.getTitle(), offer.getValidTo(),
                offer.isReservableOn(today()));
    }

    private Offer findOffer(Long offerId) {
        return offerRepository.findById(offerId).orElseThrow(() -> new OfferNotFoundException(offerId));
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(OFFER_ZONE));
    }
}
