package com.geopslabs.geops.engagement.domain.ports;

import com.geopslabs.geops.engagement.domain.models.Favorite;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepositoryPort {
    Favorite save(Favorite favorite);

    Optional<Favorite> findById(Long id);

    List<Favorite> findByUserId(Long userId);

    Optional<Favorite> findByUserIdAndOfferId(Long userId, Long offerId);

    boolean existsById(Long id);

    boolean existsByUserIdAndOfferId(Long userId, Long offerId);

    void deleteById(Long id);

    long deleteByUserIdAndOfferId(Long userId, Long offerId);
}
