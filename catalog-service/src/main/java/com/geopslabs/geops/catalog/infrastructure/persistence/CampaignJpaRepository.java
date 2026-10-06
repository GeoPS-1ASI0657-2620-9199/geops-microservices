package com.geopslabs.geops.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignJpaRepository extends JpaRepository<CampaignJpaEntity, Long> {
    Optional<CampaignJpaEntity> findCampaignById(Long id);

    void deleteCampaignById(Long id);

    List<CampaignJpaEntity> findAllByBusinessId(Long businessId);
}
