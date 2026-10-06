package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CampaignJpaAdapter implements CampaignRepositoryPort {
    private final CampaignJpaRepository repository;

    public CampaignJpaAdapter(CampaignJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Campaign save(Campaign campaign) {
        return CampaignPersistenceMapper.toDomain(repository.save(CampaignPersistenceMapper.toEntity(campaign)));
    }

    @Override
    public Optional<Campaign> findById(Long id) {
        return repository.findCampaignById(id).map(CampaignPersistenceMapper::toDomain);
    }

    @Override
    public List<Campaign> findAll() {
        return repository.findAll().stream().map(CampaignPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<Campaign> findByBusinessId(Long businessId) {
        return repository.findAllByBusinessId(businessId).stream().map(CampaignPersistenceMapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteCampaignById(id);
    }
}
