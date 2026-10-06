package com.geopslabs.geops.engagement.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedOfferJpaRepository extends JpaRepository<SavedOfferJpaEntity, Long> {
    List<SavedOfferJpaEntity> findByConsumerId(Long consumerId);

    Optional<SavedOfferJpaEntity> findByConsumerIdAndOfferId(Long consumerId, Long offerId);

    boolean existsByConsumerIdAndOfferId(Long consumerId, Long offerId);

    long deleteByConsumerIdAndOfferId(Long consumerId, Long offerId);
}

