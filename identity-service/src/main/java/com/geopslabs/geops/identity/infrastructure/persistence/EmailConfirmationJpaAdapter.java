package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.EmailConfirmation;
import com.geopslabs.geops.identity.domain.ports.EmailConfirmationRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EmailConfirmationJpaAdapter implements EmailConfirmationRepositoryPort {
    private final EmailConfirmationJpaRepository repository;

    public EmailConfirmationJpaAdapter(EmailConfirmationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(EmailConfirmation confirmation) {
        var entity = new EmailConfirmationJpaEntity();
        entity.setUserId(confirmation.userId());
        entity.setTokenHash(confirmation.tokenHash());
        entity.setExpiresAt(confirmation.expiresAt());
        entity.setCreatedAt(confirmation.createdAt());
        repository.save(entity);
    }

    @Override
    public Optional<EmailConfirmation> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(entity -> new EmailConfirmation(entity.getUserId(),
                entity.getTokenHash(), entity.getExpiresAt(), entity.getCreatedAt()));
    }

    @Override
    public void deleteByUserId(Long userId) {
        repository.deleteById(userId);
    }
}
