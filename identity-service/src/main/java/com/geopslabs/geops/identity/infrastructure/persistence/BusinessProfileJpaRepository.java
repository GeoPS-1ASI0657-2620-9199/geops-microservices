package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessProfileJpaRepository extends JpaRepository<BusinessProfileJpaEntity, Long> {
    Optional<BusinessProfileJpaEntity> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByRuc(String ruc);

    @Query("SELECT profile.id FROM BusinessProfileJpaEntity profile WHERE profile.verificationStatus = :status "
            + "ORDER BY profile.id")
    List<Long> findIdsByVerificationStatus(@Param("status") VerificationStatus status);
}
