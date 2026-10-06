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
        var entity = savedOffer.getId() == null ? new SavedOfferJpaEntity()
                : repository.findById(savedOffer.getId()).orElseGet(SavedOfferJpaEntity::new);
        return SavedOfferPersistenceMapper.toDomain(
                repository.save(SavedOfferPersistenceMapper.toEntity(savedOffer, entity)));
    }

    @Override
    public Optional<SavedOffer> findById(Long id) {
        return repository.findById(id).map(SavedOfferPersistenceMapper::toDomain);
    }

    @Override
    public List<SavedOffer> findByConsumerId(Long consumerId) {
        return repository.findByConsumerId(consumerId).stream().map(SavedOfferPersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<SavedOffer> findByConsumerIdAndOfferId(Long consumerId, Long offerId) {
        return repository.findByConsumerIdAndOfferId(consumerId, offerId).map(SavedOfferPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsByConsumerIdAndOfferId(Long consumerId, Long offerId) {
        return repository.existsByConsumerIdAndOfferId(consumerId, offerId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public long deleteByConsumerIdAndOfferId(Long consumerId, Long offerId) {
        return repository.deleteByConsumerIdAndOfferId(consumerId, offerId);
    }
}
