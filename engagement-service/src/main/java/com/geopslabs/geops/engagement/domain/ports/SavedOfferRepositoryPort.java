package com.geopslabs.geops.engagement.domain.ports;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;

import java.util.List;
import java.util.Optional;

public interface SavedOfferRepositoryPort {
    SavedOffer save(SavedOffer savedOffer);

    Optional<SavedOffer> findById(Long id);

    List<SavedOffer> findByUserId(Long userId);

    Optional<SavedOffer> findByUserIdAndOfferId(Long userId, Long offerId);

    boolean existsById(Long id);

    boolean existsByUserIdAndOfferId(Long userId, Long offerId);

    void deleteById(Long id);

    long deleteByUserIdAndOfferId(Long userId, Long offerId);
}
