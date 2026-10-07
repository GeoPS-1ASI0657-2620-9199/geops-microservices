package com.geopslabs.geops.engagement.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedOfferJpaRepository extends JpaRepository<SavedOfferJpaEntity, Long> {

    List<SavedOfferJpaEntity> findByConsumerIdOrderBySavedAtDesc(Long consumerId);

    Optional<SavedOfferJpaEntity> findByConsumerIdAndOfferId(Long consumerId, Long offerId);

    long deleteByConsumerIdAndOfferId(Long consumerId, Long offerId);
}
