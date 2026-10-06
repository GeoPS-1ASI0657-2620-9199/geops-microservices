package com.geopslabs.geops.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferJpaRepository extends JpaRepository<OfferJpaEntity, Long> {

    List<OfferJpaEntity> findByCampaign_IdOrderByIdAsc(Long campaignId);
}
