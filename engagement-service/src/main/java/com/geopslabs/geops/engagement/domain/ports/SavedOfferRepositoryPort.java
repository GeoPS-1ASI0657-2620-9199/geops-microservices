package com.geopslabs.geops.engagement.domain.ports;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;

import java.util.List;
import java.util.Optional;

public interface SavedOfferRepositoryPort {
    SavedOffer save(SavedOffer savedOffer);

    List<SavedOffer> findByConsumerId(Long consumerId);

    Optional<SavedOffer> findByConsumerIdAndOfferId(Long consumerId, Long offerId);

    long deleteByConsumerIdAndOfferId(Long consumerId, Long offerId);
}
