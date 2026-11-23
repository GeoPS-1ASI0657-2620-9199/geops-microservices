package com.geopslabs.geops.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface RevokedTokenJpaRepository extends JpaRepository<RevokedTokenJpaEntity, String> {
    @Modifying
    @Query("DELETE FROM RevokedTokenJpaEntity revoked WHERE revoked.expiresAt < :instant")
    int deleteByExpiresAtBefore(@Param("instant") Instant instant);
}
