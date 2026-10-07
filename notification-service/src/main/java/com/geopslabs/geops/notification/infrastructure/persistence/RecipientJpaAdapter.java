package com.geopslabs.geops.notification.infrastructure.persistence;

import com.geopslabs.geops.notification.domain.models.Recipient;
import com.geopslabs.geops.notification.domain.ports.RecipientRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class RecipientJpaAdapter implements RecipientRepositoryPort {
    private final RecipientJpaRepository repository;

    public RecipientJpaAdapter(RecipientJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsConsumer(Long userId) {
        return repository.existsByUserIdAndRole(userId, Recipient.CONSUMER);
    }

    @Override
    public void upsert(Recipient recipient) {
        repository.upsert(recipient.userId(), recipient.email(), recipient.emailConfirmed(), recipient.role());
    }
}
