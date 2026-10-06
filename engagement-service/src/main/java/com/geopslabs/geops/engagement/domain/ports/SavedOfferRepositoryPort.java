package com.geopslabs.geops.engagement.domain.ports;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;

import java.util.List;
import java.util.Optional;

public interface SavedOfferRepositoryPort {
    SavedOffer save(SavedOffer savedOffer);

    Optional<SavedOffer> findById(Long id);

    List<SavedOffer> findByConsumerId(Long consumerId);

    Optional<SavedOffer> findByConsumerIdAndOfferId(Long consumerId, Long offerId);

    boolean existsById(Long id);

    boolean existsByConsumerIdAndOfferId(Long consumerId, Long offerId);

    void deleteById(Long id);

    long deleteByConsumerIdAndOfferId(Long consumerId, Long offerId);
}
