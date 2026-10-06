package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class OfferJpaAdapter implements OfferRepositoryPort {
    private final OfferJpaRepository repository;

    public OfferJpaAdapter(OfferJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Offer save(Offer offer) {
        return OfferPersistenceMapper.toDomain(repository.save(OfferPersistenceMapper.toEntity(offer)));
    }

    @Override
    public Optional<Offer> findById(Long id) {
        return repository.findById(id).map(OfferPersistenceMapper::toDomain);
    }

    @Override
    public List<Offer> findAll() {
        return repository.findAll().stream().map(OfferPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<Offer> findByIdIn(List<Long> ids) {
        return repository.findByIdIn(ids).stream().map(OfferPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<Offer> findByCampaignId(Long campaignId) {
        return repository.findByCampaign_Id(campaignId).stream().map(OfferPersistenceMapper::toDomain).toList();
    }

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
