package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.RevokedToken;
import com.geopslabs.geops.identity.domain.models.TokenFingerprint;
import com.geopslabs.geops.identity.domain.ports.RevokedTokenRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RevokedTokenJpaAdapter implements RevokedTokenRepositoryPort {
    private final RevokedTokenJpaRepository repository;

    public RevokedTokenJpaAdapter(RevokedTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isRevoked(TokenFingerprint fingerprint) {
        return repository.existsById(fingerprint.value());
    }

    @Override
    public void save(RevokedToken revokedToken) {
        var entity = new RevokedTokenJpaEntity();
        entity.setFingerprint(revokedToken.fingerprint().value());
        entity.setUserId(revokedToken.userId());
        entity.setExpiresAt(revokedToken.expiresAt());
        entity.setRevokedAt(revokedToken.revokedAt());
        repository.save(entity);
    }

    @Override
    public int deleteExpiredBefore(Instant instant) {
        return repository.deleteByExpiresAtBefore(instant);
    }
}
