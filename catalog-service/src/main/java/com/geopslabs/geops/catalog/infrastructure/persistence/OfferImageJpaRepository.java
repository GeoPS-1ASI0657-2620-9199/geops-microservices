package com.geopslabs.geops.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OfferImageJpaRepository extends JpaRepository<OfferImageJpaEntity, UUID> {
}
