package com.geopslabs.geops.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferJpaRepository extends JpaRepository<OfferJpaEntity, Long> {

    List<OfferJpaEntity> findByIdIn(List<Long> ids);

    List<OfferJpaEntity> findByCampaign_Id(Long campaignId);

}
