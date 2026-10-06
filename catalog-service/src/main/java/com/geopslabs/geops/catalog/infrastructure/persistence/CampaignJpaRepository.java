package com.geopslabs.geops.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignJpaRepository extends JpaRepository<CampaignJpaEntity, Long> {

    List<CampaignJpaEntity> findByBusinessIdOrderByIdAsc(Long businessId);
}
