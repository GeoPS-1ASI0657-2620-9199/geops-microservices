package com.geopslabs.geops.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailConfirmationJpaRepository extends JpaRepository<EmailConfirmationJpaEntity, Long> {
    Optional<EmailConfirmationJpaEntity> findByTokenHash(String tokenHash);
}
