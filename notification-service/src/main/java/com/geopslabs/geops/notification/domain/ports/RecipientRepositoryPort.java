package com.geopslabs.geops.notification.domain.ports;

import com.geopslabs.geops.notification.domain.models.Recipient;

public interface RecipientRepositoryPort {
    boolean existsConsumer(Long userId);

    void upsert(Recipient recipient);

    /** Marks the email of the recipient as confirmed; false when the recipient copy does not exist yet. */
    boolean confirmEmail(Long userId, String email);
}
