package com.geopslabs.geops.identity.domain.ports;

import com.geopslabs.geops.identity.domain.models.EmailConfirmation;

import java.util.Optional;

public interface EmailConfirmationRepositoryPort {
    void save(EmailConfirmation confirmation);

    Optional<EmailConfirmation> findByTokenHash(String tokenHash);

    void deleteByUserId(Long userId);
}
