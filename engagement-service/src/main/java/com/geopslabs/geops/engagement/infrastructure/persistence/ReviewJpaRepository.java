package com.geopslabs.geops.engagement.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewJpaRepository extends JpaRepository<ReviewJpaEntity, Long> {

    List<ReviewJpaEntity> findByBusinessIdOrderByCreatedAtDescIdDesc(Long businessId);
}
