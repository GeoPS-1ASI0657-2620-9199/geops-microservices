package com.geopslabs.geops.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RucVerificationJpaRepository extends JpaRepository<RucVerificationJpaEntity, Long> {
    Optional<RucVerificationJpaEntity> findFirstByBusinessProfileIdOrderByCheckedAtDescIdDesc(Long businessProfileId);

    List<RucVerificationJpaEntity> findByBusinessProfileIdOrderByIdAsc(Long businessProfileId);
}
