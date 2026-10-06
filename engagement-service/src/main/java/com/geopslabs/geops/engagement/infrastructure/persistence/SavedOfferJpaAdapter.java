package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.ports.SavedOfferRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SavedOfferJpaAdapter implements SavedOfferRepositoryPort {
    private final SavedOfferJpaRepository repository;

    public SavedOfferJpaAdapter(SavedOfferJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public SavedOffer save(SavedOffer savedOffer) {
        var entity = SavedOfferPersistenceMapper.toEntity(savedOffer, new SavedOfferJpaEntity());
        return SavedOfferPersistenceMapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public List<SavedOffer> findByConsumerId(Long consumerId) {
        return repository.findByConsumerIdOrderBySavedAtDesc(consumerId).stream()
                .map(SavedOfferPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<SavedOffer> findByConsumerIdAndOfferId(Long consumerId, Long offerId) {
        return repository.findByConsumerIdAndOfferId(consumerId, offerId).map(SavedOfferPersistenceMapper::toDomain);
    }

    @Override
    public long deleteByConsumerIdAndOfferId(Long consumerId, Long offerId) {
        return repository.deleteByConsumerIdAndOfferId(consumerId, offerId);
    }
}
