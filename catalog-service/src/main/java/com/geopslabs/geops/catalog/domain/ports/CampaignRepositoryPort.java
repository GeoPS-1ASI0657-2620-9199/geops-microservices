package com.geopslabs.geops.catalog.domain.ports;

import com.geopslabs.geops.catalog.domain.models.Campaign;

import java.util.List;
import java.util.Optional;

public interface CampaignRepositoryPort {
    Campaign save(Campaign campaign);

    Optional<Campaign> findById(Long id);

    List<Campaign> findAll();

    List<Campaign> findByBusinessId(Long businessId);

    void deleteById(Long id);
}
