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
    public List<SavedOffer> findByUserId(Long userId) {
        return repository.findByUser_Id(userId).stream().map(SavedOfferPersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<SavedOffer> findByUserIdAndOfferId(Long userId, Long offerId) {
        return repository.findByUser_IdAndOffer_Id(userId, offerId).map(SavedOfferPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsByUserIdAndOfferId(Long userId, Long offerId) {
        return repository.existsByUser_IdAndOffer_Id(userId, offerId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public long deleteByUserIdAndOfferId(Long userId, Long offerId) {
        return repository.deleteByUser_IdAndOffer_Id(userId, offerId);
    }
}
