package com.geopslabs.geops.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsumerProfileJpaRepository extends JpaRepository<ConsumerProfileJpaEntity, Long> {
    Optional<ConsumerProfileJpaEntity> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
