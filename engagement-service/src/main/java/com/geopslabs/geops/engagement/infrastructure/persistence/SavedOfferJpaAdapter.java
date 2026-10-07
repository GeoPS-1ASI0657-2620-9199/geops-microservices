package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.models.exceptions.SavedOfferAlreadyExistsException;
import com.geopslabs.geops.engagement.domain.ports.SavedOfferRepositoryPort;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class SavedOfferJpaAdapter implements SavedOfferRepositoryPort {
    static final String SAVED_OFFER_UNIQUE_KEY = "uk_saved_offers_1";

    private final SavedOfferJpaRepository repository;

    public SavedOfferJpaAdapter(SavedOfferJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public SavedOffer save(SavedOffer savedOffer) {
        try {
            var saved = repository.saveAndFlush(SavedOfferPersistenceMapper.toEntity(savedOffer));
            return SavedOfferPersistenceMapper.toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            throw translate(exception);
        }
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
    @Transactional
    public long deleteByConsumerIdAndOfferId(Long consumerId, Long offerId) {
        return repository.deleteByConsumerIdAndOfferId(consumerId, offerId);
    }

    private static RuntimeException translate(DataIntegrityViolationException exception) {
        var cause = NestedExceptionUtils.getMostSpecificCause(exception).getMessage();
        if (cause != null && cause.contains(SAVED_OFFER_UNIQUE_KEY)) {
            return new SavedOfferAlreadyExistsException();
        }
        return exception;
    }
}
