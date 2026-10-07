package com.geopslabs.geops.notification.domain.ports;

import com.geopslabs.geops.notification.domain.models.Recipient;

public interface RecipientRepositoryPort {
    boolean existsConsumer(Long userId);

    void upsert(Recipient recipient);
}
